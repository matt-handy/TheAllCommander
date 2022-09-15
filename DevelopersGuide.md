# Command Macros
The basic idea for command macros is to add a simple, one line command to TheAllCommander which will execute several commands on the client, including commands which are dependent on the results of a prior command. For example, a macro might poll a directory for a set of files, apply some logic to determine which file is wanted, and then uplink that file. This can be done to similar a number of user-mode attack chains and build an easily scripted simulation. 

## Step 1 - Extend AbstractCommandMacro
One startup of TheAllCommander, the initialize method will be called automatically. The macro will be given an IOManager and HarvestProcessor object. The macro will need to store a reference to this IOManager for use in executing the command workflow.

This class first will implement isCommandMatch, a method which will determine if the command which was run by the user can and should be interpretted by the macro. If the macro returns "true" from this method, control flow will be handed over to processCmd method. This method takes the command given by the user, a session ID, and a session string. The session ID can be used to interact with the IOManager to send command and receive inputs back from the daemon.

## Step 2 - Compile and add to classpath
The new macro can be added to TheAllCommander's c2.session.macro package directly, or it can be independently compiled and added to TheAllCommander's execCentral classpath. 

## Step 3 - Add to configuration
The full class name of the macro needs to be added to the "macros" configuration element in test.properties.

# Additional Communication Protocols
TheAllCommander defines an abstract class c2.C2Interface, which is extended by developers to add new communication schemes to the framework. TheAllCommander, when configured, will automatically initialize the new communication protocol and start the service automatically. The developer can then extend the provided LocalAgent python daemon to implement the client end of the communication mechanism. 

## Step 1 - Extend C2Interface
The initialize method sets up the C2Interface with the IOManager, the Properties, the Keylogger Processor and Harvest Processor. The IOManager is used to return IO to the commanding session, and to receive outgoing commands for the daemon.

The C2Interface inherits "run" from Runnable. Here, the user will open any sockets or perform any other actions needed to operate. For example, for an HTTPSServer, the server runs in the run method, and as requests for commands come in, the IOManager is checked for input. Returned commands are posted to the IOManager. This example can be found at c2.http.HTTPSManager

stop() is a blocking method, which will not return until the C2Interface has full run its course and stopped all activity.

notifyPendingShutdown() is a "soft" kill signal, which TheAllCommander will use to signal an imminent shutdown. The C2Interface may ignore this signal until the stop() method is invoked, however for efficiency it is recommended to begin teardown actions.

getName() returns a human readable name for the C2Interface which is used for logging and other UI purposes.

## Step 2 - Compile and add to classpath
The new C2Interface can be added to TheAllCommander's source tree directly, or it can be independently compiled and added to TheAllCommander's execCentral classpath. 

## Step 3 - Add to configuration
The full class name of the macro needs to be added to the "commservices" configuration element in test.properties.

## Step 4 - Integration Testing
TheAllCommander defines a interface compatibility test sequence. The unit test should extend from util.test.ClientServerTest to have access to server startup and teardown. RunnerTestGeneric provides a "test" method which runs through IO with the daemon, ensuring the commands are sent, processed and replied to with accuracy. Please see c2.python.PythonHTTPSAllCommandsTest.java for an example