#!/usr/bin/env bash

TEST_CASE="${TEST_CASE_LOCATION:-./query.sql}"

output=$(sqlite3-3.26.0 < $TEST_CASE 2>&1)

if [[ $output == *"database disk image is malformed"* ]]
then
  exit 0
else
  exit 1
fi