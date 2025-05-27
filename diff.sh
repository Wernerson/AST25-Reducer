#!/usr/bin/env bash

TEST_CASE="${TEST_CASE_LOCATION:-./query.sql}"

output=$(sqlite3-3.26.0 < $TEST_CASE)
ooutput=$(sqlite3-3.39.4 < $TEST_CASE)

if [[ "$output" == "$ooutput" ]]
then
  exit 1
else
  exit 0
fi