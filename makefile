#=============================
# variables and configuration
#=============================

SHELL = /bin/bash
MAIN = Main

#=============================
#  build targets  
#=============================

all: build

build:
	if [ ! -d bin ]; then mkdir bin; fi
	javac -sourcepath src -d bin -cp $(CLASSPATH) src/core/$(MAIN).java

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
