FROM theosotr/sqlite3-test:latest
COPY ./build/native/nativeCompile/AST-reducer /usr/bin/reducer
COPY ./check.sh /home/test/check.sh
COPY ./diff.sh /home/test/diff.sh
COPY ./crash.sh /home/test/crash.sh