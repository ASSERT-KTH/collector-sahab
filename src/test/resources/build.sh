#!/bin/bash

build () {
  mvn test-compile -Dmaven.compiler.debug="$1" -DbuildDirectory="$2"
  mvn dependency:copy-dependencies -DbuildDirectory="$2"
}
