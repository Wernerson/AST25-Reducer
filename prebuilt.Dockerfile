FROM theosotr/sqlite3-test:latest
COPY ./build/native/nativeCompile/AST-fuzzer /usr/bin/test-db
COPY ./configs/ /home/test/configs/