Project 2
----------
The HTTP and SSL server are both contained in the same file, Server.java.  Since the only real difference is how the connection is set up
we simply use conditionals to decide which type of server should be run (based on the boolean mIsSecureServer). 

We have attempted the extra credit for this project.  To accomplish this, we have the Server class extend Thread.  The main work of the 
server happens in the overrided run() method.  

We have programmed the socket to timeout after 10 seconds of inactivity by the client.  After this timeout, the server will simply loop around 
and look for another connection.

The included Makefile should be used to build the project:  make clean && make

The servers can then be run (on different threads) using: java Server --serverPort=12345 --sslServerPort=23456
