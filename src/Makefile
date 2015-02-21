JAVAC = javac
JFLAGS = -g -d . -classpath .

default: all

all: Driver

Driver: Driver.java HTTPServer.java Utils.java SecureHTTPServer.java
	$(JAVAC) $(JFLAGS) Driver.java HTTPServer.java Utils.java SecureHTTPServer.java

clean:
	rm -f *.class


