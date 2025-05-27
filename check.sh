#!/usr/bin/env bash

TEST_CASE="${TEST_CASE_LOCATION:-./query.sql}"

output=$(sqlite3-3.26.0 < $TEST_CASE)
code=$?

if [ $code -ne 0 ] && [ $code -ne 1 ]
then
  echo "Funny error code"
  exit 0
else
  ooutput=$(sqlite3-3.39.4 < $TEST_CASE)
  ocode=$?

  echo $code $ocode
  if [[ "$code" != "$ocode" ]]
  then
    echo "Code not equal"
    exit 0
  fi

  if [[ "$output" == "$ooutput" ]]
  then
    echo "Output equal"
    exit 1
  else
    echo "Output unequal"
    exit 0
  fi
fi