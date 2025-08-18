## 소개
***
본 레포지토리는 [League of algoLogic](https://lab.ssafy.com/skwpqjq/c204-be-api) 프로젝트의 채점 서버 레포지토리입니다.

## 프로젝트 빌드
***
```shell
git clone https://lab.ssafy.com/skwpqjq/c204-be-judge.git

cd c204-be-judge
cp <env 파일 경로> .env

./gradlew clean build

java -jar -Dspring.profiles.active=prod c204-be-judge-0.0.1-SNAPSHOT.jar`
```

## 사전 준비
***
### 환경 구축
#### 1. 프로그래밍 언어 설치
```shell
sudo apt update
sudo apt install openjdk-17-jdk
sudo apt install g++
```
#### 2. isolate 설치
```shell
sudo mkdir -p /etc/apt/keyrings

curl https://www.ucw.cz/isolate/debian/signing-key.asc | sudo tee /etc/apt/keyrings/isolate.asc > /dev/null

echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/isolate.asc] http://www.ucw.cz/isolate/debian/ bookworm-isolate main" | sudo tee /etc/apt/sources.list.d/isolate.list

sudo apt update
sudo apt install isolate
```
#### 3. 소스코드 및 테스트케이스 복사

- 사용자의 소스코드 `(/home/ubuntu/sourceCode)`
- 채점에 사용될 테스트케이스 `(/home/ubuntu/testcases)`
- 메타 파일 `(/home/ubuntu/c204-be-judge/meta)`

#### 4. 운영체제
- `ubuntu` 환경에서만 동작 가능합니다.
