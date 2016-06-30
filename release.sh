#!/usr/bin/env bash
export JAVA_HOME=$(/usr/libexec/java_home)
mvn release:clean
mvn release:prepare
mvn release:perform