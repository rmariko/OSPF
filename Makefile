#
# A simple makefile for compiling the router class
#

#
# java compiler
#
JCC = javac

#
# Compile the sender and receiver java files - 'make'
#
default: 
	$(JCC) router.java
	
#
# Start from scratch - 'make clean'
#
clean: 
	$(RM) *.class
	$(RM) *.log