package c2;

import java.util.Properties;

import c2.session.IOManager;

public abstract class C2Interface implements Runnable{
	/**
	* Initializes the C2Interface by passing all the general communication classes for TheAllCommander 
	* This method may throw an exception if TheAllCommander has not supplied sufficient configuration information. 
	* <p>
	* Please see the documentation for IOManager, KeyloggerProcessor, and HarvestProcessor for information on how to use
	* these classes. 
	*
	* @param  io  {@link c2.session.IOManager}
	* @param  prop {@link java.util.Properties}
	* @param  keylogger {@link c2.KeyloggerProcessor}
	* @param  harverster {@link c2.HarvestProcessor}
	* @throws Exception Can be thrown if insuffient configuration information is passed via properties, or if the class
	* is not able to being operations
	*/
	public abstract void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harverster) throws Exception;
	
	/**
	* This method causes the C2Interface run() method to come to an end, and will not return until that full
	* shutdown is complete.
	*
	*/
	public abstract void stop();
	
	/**
	* This method should give a unique and human readable name for the C2Interface instance, which is used by
	* TheAllCommander for logging purposes.
	*
	* @return The string representation of the name
	*/
	public abstract String getName();
	
	/**
	* This method notifies the C2Interface that a "stop()" signal will be issued soon. It can return as soon
	* as the class registers this is happening. The class may stop "run()" at any point after "stop()" has been
	* issued, but it is not required to do so.
	*
	*/
	public abstract void notifyPendingShutdown();
	
	/**
	 * This method blocks until all threading activity necessary to start the service is complete
	 */
	public abstract void awaitStartup();
}
