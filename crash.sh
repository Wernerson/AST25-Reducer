#!/usr/bin/env bash

output=$(sqlite3-3.26.0 < $1)
code=$?

if [ $code -ne 0 ] && [ $code -ne 1 ]
then
  exit 0
else
  exit 1
fi