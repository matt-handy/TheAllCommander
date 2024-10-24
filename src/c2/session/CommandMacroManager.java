package c2.session;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import c2.Constants;
import c2.HarvestProcessor;
import c2.rdp.RDPSessionInfo;
import c2.rdp.WindowsRDPManager;
import c2.session.macro.AbstractAuditMacro;
import c2.session.macro.AbstractCommandMacro;
import c2.session.macro.MacroOutcome;
import c2.session.macro.enumeration.AuditAllTheThingsMacro;
import c2.session.macro.enumeration.WindowsAuditDllIntegrityMacro;
import c2.session.macro.enumeration.WindowsPrivescMisconfigurationAuditMacro;
import c2.session.macro.enumeration.WindowsRegistryConfigurationAuditMacro;
import c2.session.macro.enumeration.WindowsUnencryptedConfigurationPasswordAuditor;
import c2.session.macro.enumeration.cve.WindowsPrivescCVE;

/** This class processes commands received from commanding sessions before
 * they are passed to a daemon. Some commands are implemented on the server
 * side before transmission to clients, as this allows a consolidated command
 * form which is agnostic of the type of client on the receiving end
 *
*/

public class CommandMacroManager {
	
	public static final String ACTIVATE_RDP_CMD = "activate_rdp";
	
	private WindowsRDPManager manager;
	private IOManager io;
	
	private HarvestProcessor harvestProcessor;
	
	public CommandMacroManager(WindowsRDPManager manager, IOManager io, String lootLz) {
		this.manager = manager;
		this.io = io;
		harvestProcessor = new HarvestProcessor(lootLz);
	}
	
	private List<AbstractCommandMacro> macros = new ArrayList<>();
	
	public void initializeMacros(Properties properties) {
		String macroListString = properties.getProperty(Constants.MACROS);
		String[] macroClassNames = macroListString.split(",");
		for(String className : macroClassNames) {
			try {
				Class<?> c = Class.forName(className);
				Constructor<?> cons = c.getConstructor();
				Object object = cons.newInstance();
				AbstractCommandMacro newModule = (AbstractCommandMacro) object;
				newModule.initialize(io, harvestProcessor);
				macros.add(newModule);
			}catch(Exception ex) {
				System.out.println("Unable to load class: " + className);
				ex.printStackTrace();
			}
		}
		
		//Add built in Audit Macros
		WindowsPrivescCVE windowsPrivescCVEMacro = new WindowsPrivescCVE();
		windowsPrivescCVEMacro.initialize(io, harvestProcessor);
		macros.add(windowsPrivescCVEMacro);
		WindowsPrivescMisconfigurationAuditMacro privescAudit = new WindowsPrivescMisconfigurationAuditMacro();
		privescAudit.initialize(io, harvestProcessor);
		macros.add(privescAudit);
		WindowsRegistryConfigurationAuditMacro regAuditMacro = new WindowsRegistryConfigurationAuditMacro();
		regAuditMacro.initialize(io, harvestProcessor);
		macros.add(regAuditMacro);
		WindowsUnencryptedConfigurationPasswordAuditor passwordAuditMacro = new WindowsUnencryptedConfigurationPasswordAuditor();
		passwordAuditMacro.initialize(io, harvestProcessor);
		macros.add(passwordAuditMacro);
		WindowsAuditDllIntegrityMacro dllIntegrityMacro = new WindowsAuditDllIntegrityMacro();
		dllIntegrityMacro.initialize(io, harvestProcessor);
		macros.add(dllIntegrityMacro);
		
		List<AbstractAuditMacro> auditMacros = new ArrayList<>();
		auditMacros.add(windowsPrivescCVEMacro);
		auditMacros.add(privescAudit);
		auditMacros.add(regAuditMacro);
		auditMacros.add(passwordAuditMacro);
		auditMacros.add(dllIntegrityMacro);
		AuditAllTheThingsMacro mainAuditMacro = new AuditAllTheThingsMacro(auditMacros);
		mainAuditMacro.initialize(io, harvestProcessor);
		macros.add(mainAuditMacro);
	}
	
	public String getListOfMacros() {
		StringBuilder sb = new StringBuilder();
		for(AbstractCommandMacro macro : macros) {
			sb.append(macro.getReadableName());
			sb.append(System.lineSeparator());
			sb.append(macro.getInvocationCommandDescription());
			sb.append(System.lineSeparator());
			sb.append(macro.getBehaviorDescription());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
	
	public boolean processCmd(String commandString, int sessionId, String sessionStr) {
		for(AbstractCommandMacro macro : macros) {
			if(macro.isCommandMatch(commandString)) {
				MacroOutcome outcome = macro.processCmd(commandString, sessionId, sessionStr);
				if(outcome.hasErrors()) {
					io.sendIO(sessionId, "Cannot execute, errors encountered: " + System.lineSeparator());
					for(String error : outcome.getErrors()) {
						io.sendIO(sessionId, error + System.lineSeparator());
					}
				}
				for(String msg : outcome.getOutput()) {
					io.sendIO(sessionId, msg + System.lineSeparator());
				}
				return true;
			}
		}
		if(commandString.startsWith(ACTIVATE_RDP_CMD)){
			String username = commandString.substring(ACTIVATE_RDP_CMD.length() + 1);
			RDPSessionInfo info = manager.executeRDPSetup(sessionStr, username);
			if(info.hasErrors()) {
				io.sendIO(sessionId, "Cannot enable RDP, errors encountered: " + System.lineSeparator());
				for(String error : info.getErrors()) {
					io.sendIO(sessionId, error + System.lineSeparator());
				}
			}else {
				io.sendIO(sessionId, "RDP Setup Complete" + System.lineSeparator());
			}
		}else {
			return false;
		}
		//If we didn't hit the else block, we processed a command
		return true;
	}
}
