Project 2
----------
HTTP Server: Server.java
HTTPS Server: HTTPSServer.java

We have attempted the extra credit for this project.  To accomplish this, we have created an abstract
class (AbstractServer.java).  The abstract server contains the shared code for both the HTTP and HTTPS
server and extends Thread.  Both of the concrete server classes inherit from AbstractServer.java.

The main method is in Server.java so that the project can be run according to spec.

The included Makefile should be used to build the project:  make clean && make

The servers can then be run (on different threads) using: java Server --serverPort=12345 --sslServerPort=23456