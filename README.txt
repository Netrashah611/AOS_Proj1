Steps to run the project:
_______________________________________________________________________________________________________
	1.	Login to utd server with ssh using hostid: dc01.udallas.edu, Username:Netid and Password	
	2.	Extract the aos_project1.zip file 
	3. 	Copy the project zip file from the remote desktop
		scp -r /Users/netra/Downloads/aos_project1  nps210000@dc01.utdallas.edu:/home/013/n/np/nps210000
	4. 	Compile the code using command: javac Application.java
	5. 	Class files are generated in the same folder
	6. 	run : sh run_project.sh {NODE ID} config.txt in each node
			dc01.utdalls.edu
			dc02.utdalls.edu
			dc03.utdallas.edu
			dc04.utdallas.edu

	Note : 	Add IP address, port number in each config files 
			example: 	# nodeID hostName listen Port
						0 10.176.69.32 3332		
 
	7. Config Output files are generated in the same  folder.
		Output can be verified in the each of the output files of the respective nodes
		config_{Nodeid}.out
