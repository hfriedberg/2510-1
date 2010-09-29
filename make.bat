@echo off

REM ######## Variables
set CLASSP=.;lib\*
set MAIN=Main
REM #######################

if "%1" == "" goto make
if "%1" == "run" goto run
if "%1" == "clean" goto clean

REM # builds the project
REM ####################
:make
echo make
if not exist bin\ mkdir bin
javac -sourcepath src -d bin -classpath %CLASSP% src/core/%MAIN%.java
goto end

rem # runs the project
REM ##################
:run
echo run
java -cp bin;%CLASSP% core.%MAIN%
goto end

rem # cleans generated files
REM ########################
:clean
echo clean
rmdir /s /q bin
goto end

:end
echo.
PAUSE