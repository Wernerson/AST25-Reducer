#!/usr/bin/env bash

output=$(sqlite3-3.26.0 < $1)
code=$?

if [ $code -ne 0 ] && [ $code -ne 1 ]
then
  echo "Funny error code"
  exit 0
else
  ooutput=$(sqlite3-3.39.4 < $1)
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