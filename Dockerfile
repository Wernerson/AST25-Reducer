FROM theosotr/sqlite3-test:latest
# Install prerequisites & dependencies
RUN sudo apt install wget unzip -y

# Copy the source code to build the reducer
RUN mkdir /home/test/code
COPY ./src.zip /home/test/code/src.zip
RUN cd /home/test/code &&  \
    unzip src.zip && \
    bash install.sh