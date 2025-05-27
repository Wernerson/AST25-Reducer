#!/usr/bin/env bash

TEST_CASE="${TEST_CASE_LOCATION:-./query.sql}"

output=$(sqlite3-3.26.0 < $TEST_CASE)
code=$?

if [ $code -ne 0 ] && [ $code -ne 1 ]
then
  exit 0
else
  exit 1
fi