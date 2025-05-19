#!/usr/bin/env bash
# This script is used inside the docker container to install graalvm, build and install the fuzzer
sudo apt install curl unzip zip build-essential zlib1g-dev -y
curl -s "https://get.sdkman.io" | bash
source /home/test/.sdkman/bin/sdkman-init.sh
sdk install java 23.0.2-graal
./gradlew nativeCompile
sudo cp ./build/native/nativeCompile/AST-fuzzer /usr/bin/test-db
sudo cp -r ./configs/ ../configs/