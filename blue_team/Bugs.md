# Disclosures and Bugs

## SDCLT UAC Bypass - Windows Defender Protection Incomplete (MSRC Case 90284 / VULN-132389)

SDCLT.exe, when executed, will run another program as high integrity, bypassing UAC, if the registry key HKCU\Software\Classes\Folder\shell\open\command is set up with another executable and "Delegate Execute". Prior to August 2024, when this bypass was used, Windows Defender would rapidly kill the maliciously elevated process. However, any processes that the first process created before being terminated would be allowed to persist indefinitely unchecked. When I reported this to Microsoft, they responded with an indication that it did not warrant an immediate fix since no security boundary was violated. Testing on 8/30/24 with the most recent patched version of Windows indicates that the signature title of the Defender intercept label has changed, and now the malicious process is stopped before it can do any harm. It appears to be fully mitigated!

Please note that if Defender is disabled the UAC bypass will still work. If any other EDR solutions are used in place of Windows Defender, please confirm that detection and prevention is properly configured.  