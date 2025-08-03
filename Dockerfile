FROM ubuntu:24.04

RUN apt update && apt install -y \
    openjdk-17-jdk \
    curl \
    passwd \

RUN mkdir -p /etc/apt/keyrings \
    && curl https://www.ucw.cz/isolate/debian/signing-key.asc | tee /etc/apt/keyrings/isolate.asc > /dev/null \
    && echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/isolate.asc] http://www.ucw.cz/isolate/debian/ bookworm-isolate main" | tee /etc/apt/sources.list.d/isolate.list

RUN apt update && apt install -y isolate

ARG JAR_FILE="build/libs/c204-be-judge-0.0.1-SNAPSHOT.jar"
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
