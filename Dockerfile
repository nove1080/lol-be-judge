FROM ubuntu:24.04

# 필수 패키지 설치
RUN apt update && apt install -y \
    openjdk-17-jdk \
    curl \
    passwd

# isolate 저장소 키 추가 및 저장소 등록
RUN mkdir -p /etc/apt/keyrings \
    && curl https://www.ucw.cz/isolate/debian/signing-key.asc | tee /etc/apt/keyrings/isolate.asc > /dev/null \
    && echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/isolate.asc] http://www.ucw.cz/isolate/debian/ bookworm-isolate main" | tee /etc/apt/sources.list.d/isolate.list

# isolate 설치
RUN apt update && apt install -y isolate

ARG JAR_FILE="build/libs/c204-be-judge-0.0.1-SNAPSHOT.jar"
COPY ${JAR_FILE} c204-be-judge.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "c204-be-judge.jar"]
