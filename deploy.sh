#!/bin/bash
HOME_DIR=/home/ubuntu
GITLAB_PROJECT_NAME=c204-be-judge
PROFILE=prod

echo "> 현재 구동중인 애플리케이션 pid 확인"
CURRENT_PID=$(pgrep -f ${PROJECT_NAME}.*\jar)

echo "현재 구동 중인 애플리케이션 pid: $CURRENT_PID"
if [ -z "$CURRENT_PID" ]; then
        echo "> 현재 구동 중인 애플리케이션이 없으므로 종료하지 않습니다."
else
        echo "> kill -15 $CURRENT_PID"
        kill -15 $CURRENT_PID
        sleep 5
fi

echo "> 새 애플리케이션 배포"
JAR_NAME=$(ls -tr $HOME_DIR/$GITLAB_PROJECT_NAME/*.jar | grep -v "plain" | tail -n 1)

echo "> JAR Name: $JAR_NAME"
nohup java -jar -Dspring.profiles.active=$PROFILE $JAR_NAME > $HOME_DIR/$GITLAB_PROJECT_NAME/nohup.out 2>&1 &
