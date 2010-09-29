#=============================
# variables and configuration
#=============================

SHELL = /bin/bash
MAIN = Main
CLASSPATH = lib/*:

#=============================
#  build targets  
#=============================

all: build

build:
	javac -sourcepath src -d bin -cp $(CLASSPATH) core/$(MAIN).java

run:
	java -cp bin:$(CLASSPATH) core.$(MAIN)


#=============================
#  other targets  
#=============================

# removes all classfiles
# and the bin directory
clean:
	rm -rf bin/

rebuild: clean build
