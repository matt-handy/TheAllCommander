package c2.session.macro;

import c2.HarvestProcessor;
import c2.session.IOManager;

public abstract class AbstractCommandMacro {

	protected IOManager io;
	protected HarvestProcessor harvestProcessor;
	
	/**
	* Returns the human readable name of this macro 
	*
	* @return String containing desired information
	*/
	public abstract String getReadableName();
	
	/**
	* Returns the templated format of the macro command's invocation, such that invocation according to this format
	* will be recognized by isCommandMatch.
	*
	* @return String containing desired information
	*/
	public abstract String getInvocationCommandDescription();
	
	/**
	* Returns a brief human readable description of what the macro function does.
	*
	* @return String containing desired information
	*/
	public abstract String getBehaviorDescription();
	
	/**
	* This method allows the AbstractCommandMacro instance to test if the specified command applies to it. The entire
	* command is passed to the class, allowing for arbitrary filtering, based on the root command and any potential 
	* arguments that might be included in the command. 
	*
	* @param  cmd  The command to be tested
	* @return true/false if the command applies
	*/
	public abstract boolean isCommandMatch(String cmd);
	
	/**
	* Initializes the AbstractCommandMacro by passing all the general communication classes for TheAllCommander 
	* This method may throw an exception if TheAllCommander has not supplied sufficient configuration information. 
	* <p>
	* Please see the documentation for IOManager, and HarvestProcessor for information on how to use
	* these classes. 
	*
	* @param  io  {@link c2.session.IOManager}
	* @param  harvestProcessor {@link c2.HarvestProcessor}
	*/
	public final void initialize(IOManager io, HarvestProcessor harvestProcessor) {
		this.io = io;
		this.harvestProcessor = harvestProcessor;
	}
	/**
	* TheAllCommander will call processCmd if the instance has responded in the affirmative to "isCommandMatch".
	* At this point, the AbstractCommandMacro will translate the command into any discrete commands which are to be
	* sent to the client. 
	* <p>
	* For example, if the macro is responsible for reading a series of files, the macro would generate a series of 
	* "cat" commands for each desired file. TheAllCommander would then translate "cat" as necessary to the target OS
	* of the receiving daemon. 
	* <p>
	* As the command is processed, this method will use the MacroOutcome object to build an ordered set of commands and 
	* responses. Error states are logged here. TheAllCommander will use the MacroOutcome object to report status to the
	* user
	*
	* @param  cmd  The command from the end user, to be translated
	* @param  sessionId  The ID which can be used to send IO to and from the IOManager
	* @param  sessionStr  The fully qualified UID for the session 
	* @return returns a MacroOutcome object which contains all the commands and responses, as well as any errors
	*/
	public abstract MacroOutcome processCmd(String cmd, int sessionId, String sessionStr);

	/**
	* Implementations of AbstractCommandMacro should log all commands and responses in the MacroOutcome object. 
	* This method wraps sending commands to the IOManager and logs all commands 
	*
	* @param  cmd  The command to be sent to the daemon
	* @param  sessionId  The ID which can be used to send IO to and from the IOManager
	* @param  sessionStr The MacroOutcome object for logging 
	* @return returns a MacroOutcome object which contains all the commands and responses, as well as any errors
	*/
	protected void sendCommand(String cmd, int sessionId, MacroOutcome outcome) {
		io.sendCommand(sessionId, cmd);
		outcome.addSentCommand(cmd);
	}
	
	/**
	* Implementations of AbstractCommandMacro should log all commands and responses in the MacroOutcome object. 
	* This method wraps reception of output from the IOManager via the awaitMultilineCommands method. 
	*
	* @param  sessionId  The ID which can be used to send IO to and from the IOManager
	* @param  sessionStr The MacroOutcome object for logging 
	* @return returns a String object containing any output received
	*/
	protected String awaitResponse(int sessionId, MacroOutcome outcome) {
		String response = io.awaitMultilineCommands(sessionId);
		outcome.addResponseIo(response);
		return response;
	}
	
	/**
	* Implementations of AbstractCommandMacro should log all commands and responses in the MacroOutcome object. 
	* This method wraps reception of output from the IOManager via the awaitMultilineCommands method. 
	*
	* @param  sessionId  The ID which can be used to send IO to and from the IOManager
	* @param  sessionStr The MacroOutcome object for logging 
	* @param  timeout The number of milliseconds which should be waited by awaitMultilineCommands
	* @return returns a String object containing any output received
	*/
	protected String awaitResponse(int sessionId, MacroOutcome outcome, int timeout) {
		String response = io.awaitMultilineCommands(sessionId, timeout);
		outcome.addResponseIo(response);
		return response;
	}
}
