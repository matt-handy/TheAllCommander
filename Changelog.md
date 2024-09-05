# Release 0.9.0 - February 2022
Initial functionality

# Release 0.9.1 - March 2022
Release support for staged C# payload demonstration with "Hello World" example.

Correct threading defect in the Socks proxy test harness that caused unit test to erroneously hang

# Release 0.9.2 - August 2022
Update Email Daemon support for new Windows cURL formatting changes

Implement hosting of child shells by daemons

Implement Recycle Bin cleaning IOC 

Implement in-band data exfiltration for all platforms

# Release 0.9.3 - September 2022
Improve unit test framework - integrate full command validation for Python HTTPS and DNS daemons, as well as other integrated macro tests

Add Developers Guide

Commands as-run auditing

# Release 0.9.4 - November 2022
Add initial Blue Team Guide - provides Indicator of Compromise (IOC) Detection Recommendations for all client side attack emulation.

# Release 1.0.0 - December 2022
Clean up code quality, improve unit test coverage. Add Codira code quality badge.

# Release 2.0.0 - January 2024
Add "harvest_pwd" command - daemons will crawl current working directory for content to upload

Native text shells previously were not dynamically responsive to network latency, now will respond dynamically to data availability

Java Stager Support - Staged java payloads are now generated and supported

Mac Support - Python and Server will both run on Mac platforms

C# stager generation contains a full end-to-end integration test

C# stager code can be generated with automatic randomization and obfuscation

Blue Team Guide expanded to include several IOCs, including

	Windows registry key persistence
	
	Launch daemon process using Windows Local Machine registry for process debugging
	
	Launch daemon process using the SilentProcessExit registry key
	
Refactored harvest_user_dir implementation for streamlined maintenance

Expand user enumeration to non-Windows platforms

Windows Powershell reverse shell support added

Obfuscated powershell mode

Obfuscated Windows command line mode


# Release 2.0.1 - January 2024
Add support for secure commander sessions, unencrypted command sessions will be deprecated later.

# Release 2.1 - April 2024

Support for client telemetry gathering

Windows 11 Test suite support

PyInstaller and Windows service installation for Python daemons - Alpha Release

# Release 2.1.1 - June 2024

Add CMSTP UAC bypass macro and Winlogon persistence macro

# Release 2.1.2 - September 2024

Add SDCLT UAC bypass macro
Add Event Viewer registry key UAC bypass macro
Add support for one-time commands to be added by macros
Alpha feature - adds audit commands
Alpha feature - improved support for Python service testing