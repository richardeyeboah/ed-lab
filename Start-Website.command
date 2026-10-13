#!/bin/sh
set -e
cd "$(dirname "$0")"
# Reuse an already running website rather than starting a duplicate server.
if curl -fsS 'http://127.0.0.1:8765/api/source' 2>/dev/null | head -n 1 | grep -q 'import java.time'; then
 open 'http://127.0.0.1:8765'
 exit 0
fi
# Use a configured JDK, the Mac's installed JDK, or this Mac's preserved runtime.
ED_JAVA_HOME="${JAVA_HOME:-}"
if [ ! -x "$ED_JAVA_HOME/bin/javac" ]; then
 ED_JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
fi
if [ ! -x "$ED_JAVA_HOME/bin/javac" ]; then
 ED_JAVA_HOME="$(pwd)/.runtime"
fi
if [ ! -x "$ED_JAVA_HOME/bin/javac" ]; then
 echo 'A JDK 17 or newer is required. Set JAVA_HOME to its folder.'
 exit 1
fi
mkdir -p out
"$ED_JAVA_HOME/bin/javac" --release 17 -d out src/*.java
# The server runs on this Mac only. Keep this terminal open while using the website.
open 'http://127.0.0.1:8765'
"$ED_JAVA_HOME/bin/java" -cp out WebsiteServer
