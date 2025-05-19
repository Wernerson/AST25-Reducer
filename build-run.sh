export JAVA_HOME=../.jdks/graalvm-jdk-23.0.2 && \
./gradlew nativeCompile && \
podman build -f ./prebuilt.Dockerfile -t fuzzer-prebuilt && \
podman run -v .docker/:/home/test/.docker -it fuzzer-prebuilt