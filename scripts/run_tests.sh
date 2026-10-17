#!/bin/sh
set -e
cd "$(dirname "$0")/.."
rm -rf out && mkdir out
javac -d out $(find src -name "*.java")
java -cp out tests.AllTests
