import time

import win32serviceutil  # ServiceFramework and commandline helper
import win32service  # Events
import servicemanager  # Simple setup and logging
from telemHttpsAgentDef import TelemHTTPSAgent

#Can be bundled with the command: pyinstaller.exe --onefile --runtime-tmpdir=. --hidden-import win32timezone telemHttpsAgentService.py
#Credit where it's due: https://metallapan.se/post/windows-service-pywin32-pyinstaller/	
class TelemServiceFramework(win32serviceutil.ServiceFramework):

	_svc_name_ = 'Telem Service'
	_svc_display_name_ = 'Telem Service'

	def SvcStop(self):
		"""Stop the service"""
		self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
		self.service_impl.haltOperation()
		self.ReportServiceStatus(win32service.SERVICE_STOPPED)

	def SvcDoRun(self):
		"""Start the service; does not return until stopped"""
		self.ReportServiceStatus(win32service.SERVICE_START_PENDING)
		self.service_impl = TelemHTTPSAgent()
		self.ReportServiceStatus(win32service.SERVICE_RUNNING)
		# Run the service
		self.service_impl.runTlm(False, False)


def init():
	if len(sys.argv) == 1:
		servicemanager.Initialize()
		servicemanager.PrepareToHostSingle(TelemServiceFramework)
		servicemanager.StartServiceCtrlDispatcher()
	else:
		win32serviceutil.HandleCommandLine(TelemServiceFramework)


if __name__ == '__main__':
	init()

