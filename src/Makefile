JAVAC = javac
JFLAGS = -g -d . -classpath .

default: all

all: Server

Server: Server.java Utils.java HTTPSServer.java
	$(JAVAC) $(JFLAGS) Server.java Utils.java HTTPSServer.java

clean:
	rm -f *.class


