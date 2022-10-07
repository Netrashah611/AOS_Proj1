================---------Prerequisites:---------================================================

Softwares that are required: Java 1.8 
			     Winscp - Transfer file into server
			     Putty - Run the code in the DC machine
-----------------Steps for Execution:--------------------
1.Login	 to Winscp
using hostid: dc01.udallas.edu
      username:Netid,Password	


2.Download the Project.zip file and copy using Winscp to the respective Server.(Here : dc01/Project folder)
3. Extract the Project.zip file (This should extract to two folders
      a. Source code (This should contain all the java files required for the project)
      b. Bin (This is a precompiled class files for above java files - This folder can be directly used for running the final outputs using .sh file)
Note :To regenerate from (a) use command prompt and redirect to the Project/SourceCode folder and run below command.
command: javac *.java

4.It generates the class files in the bin folder
5.Redirect to bin folder using command prompt.

5.Now open Putty to login into dc machine (Servers : In this case i have choosen 4 servers as stated below)
	Enter Hostname as:dc01.utdallas.edu
	Based on the number of nodes considered we have to open equal number of servers (4 in this case)
 	If we consider 4 nodes open similar steps
	dc01.utdalls.edu
	dc02.utdalls.edu
	dc03.utdallas.edu
	dc04.utdallas.edu
7.Consider dc01.utdalls.edu as node 0,dc02.utdalls.edu as node 1,dc03.utdalls.edu as node 2,dc04.utdalls.edu as node 3 and run the following command:

a. Set the path in command prompt to the Project folder downloaded on the server 
Command: cd Project

b. run_project.sh is launcher file that is used to run, pass the required parameters.
 
c. Run this particular command in each of the 4 servers for 4 nodes with different config files.
run_project.sh {node_id} {config path}	
================================MAIN SCRIPT===================
sh run_project.sh 0 config.txt
sh run_project.sh 1 config.txt
sh run_project.sh 2 config.txt 
sh run_project.sh 3 config.txt
===============================END MAIN SCRIPT==================== 

8. Config Output files are generated in the current project folder (bin folder)
   Output is verifyable in the each of the output files of the respective nodes in the format 
      config-<Nodeid>.out



==================-------PROJECT Source Code java files details--------===============================

Application.java: 
	Main class which contains the main method to run the program.
		1. It establishes connection with the neighbors and adds entries for connection sockets, input streams and output streams in global maps.
		2. Requests socket connection to the input neighbors and adds entries to global maps
		3. Launches and returns thread handle to message receiver and sender thread.
		4. Launches and returns thread handle to snapshot capturing thread.



GlobalConfiguration.java: 
	1.Contains Global variables for Environment, Snapshot and Messages along with getter and setter methods to access them from any scope. 
	2. It contains a reset_snap function which resets all snapshot variables.


Listener.java:
	Runnable class which generates threads to handle connection requests from other nodes. Accepts a connection and adds it into global connection maps.


local_state.java:
	Class that stores the local state of a node along with getter and setter methods to access the variables from any scope.


MessageModel.java:
	Model class which represent the messages sent and recieved by the nodes.


NetworkOperations.java:
	Class to store and access the network communication variables and access them from any scope


NodeDetails.java:
	Class which represents node connection information


ProjectConfig.java:
	1. Class to read configurations and setup necessary environment variables
	2. Reads configuration files and sets up environment (machine name, port number, other machines in the system)

recvMsg.java:
	1. Receiver thread for the node which receives tokens sent to a given input stream. It processes the received tokens and forwards accordingly.
	2.Processes incoming snapshot reply message. 
		If type LOCAL_STATE - Adds received payload to the global payload list 
		If type IGNORED - ignore the message When all the expected replies are received, send the consolidated payload to the node from which it received the marker message.
	3. Processes incoming application message. Merge the piggybacked vector clock from the message and become active if node satisfies predefined criteria
	4.  Processes incoming marker message. If it is a valid marker message, broadcast it to other neighbors, else discard.
	5. Merge the incoming message's piggybacked vector clock into own global clock
	6. Launch SnapshotSender thread and wait for it to finish. It sends given snapshot reply message to given node.

SendMessage.java:
	1. Runnable Class which starts a thread to randomly select a neighbour and send messages on any available stream when the node is active.
	2. Function which selects a neighbor randomly and sends message to it. The execution is stopped if total sent messages reach the max limit.
	3. Contains a function to generate randomly generated neighbour id

StartServer.java:
	1. Initialize socket and input stream to start a server to host a connection
	2. Starts the server and takes input from client, closes the connection if "Over" is sent by the client.

TermDetector.java:
	Termination Detector thread which terminates the system when termination message is detected.
	


==================================================================================

