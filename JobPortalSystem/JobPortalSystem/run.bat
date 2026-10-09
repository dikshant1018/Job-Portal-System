@echo off
rem Compiles and runs JobPortal. Needs JDK 17+ and a mysql-connector-j jar in the lib folder.
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d out @sources.txt || exit /b 1
java -cp "out;lib\*" com.jobportal.ui.MainApp
