#!/usr/bin/env bash

output=$(sqlite3-3.26.0 < $1)
ooutput=$(sqlite3-3.39.4 < $1)

if [[ "$output" == "$ooutput" ]]
then
  exit 1
else
  exit 0
fi