#!/usr/bin/bash

find src/main -name "*.java" > sources.txt

javac -g -cp $(cat classpath.txt) @sources.txt -d target

trap "rm sources.txt" EXIT
