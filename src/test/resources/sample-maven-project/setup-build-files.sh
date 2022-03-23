#!/bin/bash

build () {
  mvn test-compile -Dmaven.compiler.debug="$1" -DbuildDirectory="$2"
  mvn dependency:copy-dependencies -DbuildDirectory="$2"
}

# Setup build directory with compilation information
build true with-debug

# Setup build directory without compilation information
build false without-debug
