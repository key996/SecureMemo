#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
#

APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P )
APP_HOME=$( cd "${APP_HOME:-.}" && pwd -P )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

exec "$JAVACMD" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
