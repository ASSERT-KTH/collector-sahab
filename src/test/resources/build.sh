#!/bin/bash

build () {
  mvn test-compile -Dmaven.compiler.debug="$1" -DbuildDirectory="$2"
  mvn dependency:build-classpath -Dmdep.outputFile="$2/cp.txt"
}
