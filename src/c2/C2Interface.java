package c2;

import java.util.Properties;

import c2.session.IOManager;

public abstract class C2Interface implements Runnable{
	public abstract void initialize(IOManager io, Properties prop, KeyloggerProcessor keylogger, HarvestProcessor harverster) throws Exception;
	
	public abstract void stop();
	
	public abstract String getName();
	
	public abstract void notifyPendingShutdown();
}
