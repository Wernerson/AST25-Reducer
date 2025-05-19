FROM theosotr/sqlite3-test:latest
# Download, build and install AST25-fuzzer tool as test-db
RUN git clone https://github.com/Wernerson/AST25-Fuzzer.git &&  \
    cd AST25-Fuzzer &&  \
    bash ./install.sh
