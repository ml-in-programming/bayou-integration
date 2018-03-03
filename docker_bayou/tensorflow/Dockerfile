FROM ubuntu:latest

RUN apt-get update
RUN apt-get install openjdk-8-jdk maven -y
RUN apt-get install python3 python3-pip -y && pip3 install --upgrade pip
RUN apt-get install git -y
RUN pip3 install 'tensorflow==1.2' 'scikit-learn' 'scipy'
RUN git clone https://github.com/capergroup/bayou.git
WORKDIR bayou
WORKDIR tool_files/build_scripts
RUN ./build.sh
WORKDIR out
ADD start_tensorflow.sh start_tensorflow.sh
RUN chmod +x ./start_tensorflow.sh

EXPOSE 8084 8084

CMD ./start_tensorflow.sh
