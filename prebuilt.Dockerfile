FROM theosotr/sqlite3-test:latest
COPY ./build/native/nativeCompile/AST-reducer /usr/bin/reducer
COPY ./check.sh /home/test/check.sh