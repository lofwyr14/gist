#!/bin/bash

# Writes files with 1 GB size into the current directory and checks the sha checksum later.
# You may run the read check later explicitly with the written test-xxxxx.shasum FILE.
# I thinks is similar to https://github.com/AltraMayor/f3
# Main advantage for this script:
# - needs not compiler, just "bash", "openssl", "shasum" and default utils.
# - runs much faster on my system
#
# Usage: ./my-f3-check.sh
# check free local space to find the number of files

echo "my-f3-check.sh version 2.0.1"

FREE_K=$(df -k .|tail -n 1|awk '{print $4}')
FREE=$((FREE_K * 1024))
echo "Found $(printf "%'3.d\n" $FREE) free bytes here"

PREFIX="test"
LAST=0
ERROR=0
COUNT_WRITE=0
COUNT_READ=0

# 100000 = 100 TB - limit for sure
MAX=100000

echo "Writing some files with 1GB = 1,000,000,000 bytes each"

for ((i = 0; i < MAX; i++)); do

  FREE_K=$(df -k .|tail -n 1|awk '{print $4}')

  if [ "${FREE_K}" -lt 1200000 ]; then
    break
  fi

  FILE=${PREFIX}-$(printf "%05d" "$i")

  if [ -f "${FILE}" ]; then
    echo "found existing file ${FILE} skipping name and try next"
    continue
  fi

  echo -n "write file ${FILE} ...       "
  openssl rand 1000000000 | tee ${FILE} | shasum | sed s/-/${FILE}/g >${FILE}.shasum
  ((COUNT_WRITE++));
  echo $((SECONDS - LAST)) s
  LAST=$SECONDS

done

WRITE=${SECONDS}

for FILE in ${PREFIX}-*.shasum; do
  echo -n "check FILE ${FILE} ... "
  shasum -c ${FILE} -s
  RESULT=$?
  if [ "${RESULT}" -eq "0" ]; then
    echo -n "OK    "
  else
    echo -n "ERROR "
  fi
  ((COUNT_READ++));
  ERROR=$((ERROR + RESULT))
  echo "$((SECONDS - LAST)) s"
  LAST=$SECONDS
done

READ=$((SECONDS - WRITE))

echo
echo "Found ${ERROR} errors from ${COUNT_READ} tests: $((ERROR * 100 / COUNT_READ)) %"
echo
if [ "$WRITE" -gt 0 ]; then
  echo "Writing + random + checksum performance: $((1000 * COUNT_WRITE / WRITE)) MB/s"
fi
if [ "$READ" -gt 0 ]; then
  echo "Reading + checksum          performance: $((1000 * COUNT_READ / READ)) MB/s"
fi

if [ $ERROR -gt 0 ]; then
  echo
  echo "*************** check failed! *******************"
  echo
fi
