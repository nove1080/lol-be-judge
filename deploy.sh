#!/bin/bash
PROJECT_NAME=c204-be-judge
IMAGE_NAME=nove1080/c204-be-judge:latest
CONTAINER_NAME=c204-be-judge
SERVER_PORT=8082
TESTCASES_DIR=~/testcases

echo "> 기존 Docker 컨테이너 확인"
EXISTING_CONTAINER_ID=$(docker ps -a -q --filter "name=$CONTAINER_NAME")

if [ -n "$EXISTING_CONTAINER_ID" ]; then
  echo "> 컨테이너 중지 및 삭제: $CONTAINER_NAME"
  docker stop $CONTAINER_NAME
  docker rm $CONTAINER_NAME
else
  echo "> 실행 중인 컨테이너 없음"
fi

echo "> Docker 이미지 확인 및 실행"
docker images | grep $PROJECT_NAME

echo "> 최신 Docker 이미지 Pull"
docker pull $IMAGE_NAME

echo "> 새로운 컨테이너 실행"
docker run -d \
  --name $CONTAINER_NAME \
  --env-file ./.env \
  -p $SERVER_PORT:$SERVER_PORT \
  -v $TESTCASES_DIR:$TESTCASES_DIR \
  $IMAGE_NAME

echo "> 불필요한 Docker 이미지 삭제"
docker image prune -f

echo "> 배포 완료"
