export JAVA_HOME=../.jdks/graalvm-jdk-23.0.2 && \
./gradlew nativeCompile && \
podman build -f ./prebuilt.Dockerfile -t reducer-prebuilt && \
podman run -v ./queries/:/home/test/queries:ro -it reducer-prebuilt reducer