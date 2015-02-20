JAVAC = javac
JFLAGS = -g -d . -classpath .

default: all

all: Server 

Server: Server.java Utils.java
	$(JAVAC) $(JFLAGS) Server.java Utils.java

clean:
	rm -f *.class


