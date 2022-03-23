#!/bin/bash

build () {
  mvn test-compile -Dmaven.compiler.debug="$1" -DbuildDirectory="$2"
  mvn dependency:copy-dependencies -DbuildDirectory="$2"
}

SCRIPT_DIR=$(dirname "$0")

# Setup build directory with compilation information
(cd "$SCRIPT_DIR" && build true with-debug)

# Setup build directory without compilation information
(cd "$SCRIPT_DIR" && build false without-debug)
