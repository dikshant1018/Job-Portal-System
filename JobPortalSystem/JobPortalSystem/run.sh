#!/bin/sh
# Compiles and runs JobPortal. Needs JDK 17+ and a mysql-connector-j jar in ./lib
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name '*.java') || exit 1
java -cp "out:lib/*" com.jobportal.ui.MainApp
