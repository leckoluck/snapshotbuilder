#!/usr/bin/env sh
set -e

export JAVA_OPTIONS="$JAVA_OPTIONS -Xmx1024M"

export CP=`find "../lib" -name '*.jar' | xargs echo | tr ' ' ':'`
export JAR=`find "../" -name 'snapshotbuilder.jar' | xargs echo | tr ' ' ':'`

java -classpath $CP:$JAR $JAVA_OPTIONS lecko.snapshots.SnapshotBuilder $@