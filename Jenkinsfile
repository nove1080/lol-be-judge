pipeline {
   agent any
   environment {
       deploy_server = 'i13c204.p.ssafy.io'
       user_name = 'ubuntu'
       deploy_dir = '/home/ubuntu/c204-be-judge'
       project_name = 'c204-be-judge'
       deploy_script = 'deploy.sh'

       git_url = 'https://lab.ssafy.com/skwpqjq/c204-be-judge.git'
       gitlab_credentials_id = 'ed44074e-c50d-4d12-aee8-c629099c9dec'
       branch = 'master'

       docker_hub_credentials_id = 'DOCKER_HUB_CREDENTIALS_ID'
       docker_hub_id = 'nove1080'
       docker_image_tag = 'latest'

       mattermost_url = 'https://meeting.ssafy.com/hooks/hraap7ebiige9qf15dk8k6jtwy'
       mattermost_channel = 'C204-Build-Result'
   }
   stages {
       stage('Clone Repository') {
           steps {
               echo 'Clone the Repository...'
               dir('./be/judge') {
                   git branch: branch, url: git_url, credentialsId: gitlab_credentials_id
               }
           }
       }
       stage('Prepare Secret File') {
           steps {
               echo 'copy secret file...'
               dir('./be/judge') {
                   withCredentials([file(credentialsId: 'env', variable: 'ENV')]) {
                       sh 'sudo cp $ENV .env'
                   }
               }
           }
       }
       stage('Test') {
           steps {
               echo 'Running tests...'
               dir('./be/judge') {
                   sh 'chmod +x ./gradlew'
                   sh './gradlew test'
               }
           }
       }
       stage('Build Project') {
           steps {
               echo 'build project...'
               dir('./be/judge') {
                   sh 'chmod +x ./gradlew'
                   sh './gradlew clean build -x test'
               }
           }
       }
       stage('Build Dockerfile') {
           steps {
               echo 'build dockerfile...'
               dir('./be/judge') {
                   script {
                       dockerImage = docker.build("${docker_hub_id}/${project_name}:${docker_image_tag}")
                   }
               }
           }
       }
       stage('Push Docker Image') {
           steps {
               echo 'pushing docker image to dockerhub...'
               dir('./be/judge') {
                   script {
                       docker.withRegistry('', "$docker_hub_credentials_id") {
                           dockerImage.push()
                       }
                   }
               }
           }
       }
       stage('Send .env & deploy.sh File To Deploy Server') {
           steps {
               echo 'send .env & deploy.sh file to deploy server...'
               withCredentials([file(credentialsId: 'env', variable: 'ENV')]) {
                   sshagent(credentials: ['JUDGE_SERVER_AWS_KEY']) {
                       sh '''#!/bin/bash
                           mkdir -p ~/.ssh
                           chmod 700 ~/.ssh

                           ssh-keyscan $deploy_server >> ~/.ssh/known_hosts
                           chmod 644 ~/.ssh/known_hosts

                           ssh $user_name@$deploy_server "rm -f $deploy_dir/.env"
                           scp $ENV $user_name@$deploy_server:$deploy_dir/.env
                           scp $WORKSPACE/be/judge/$deploy_script $user_name@$deploy_server:$deploy_dir
                       '''
                   }
               }
           }
       }
       stage('Deploy To AWS') {
           steps {
               echo 'deploy...'
               sshagent(credentials: ['JUDGE_SERVER_AWS_KEY']) {
                   sh '''
                       ssh -o StrictHostKeyChecking=no $user_name@$deploy_server "cd $deploy_dir && bash $deploy_script"
                   '''
               }
           }
       }
   }
   post {
       success {
           script {
               dir('./be/judge') {
                   def author = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                   def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                   def msg = "🖥️ [Back-end: Judge Server]\n" +
                             "✅ [Build Success] <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>\n" +
                             "👨‍💻 Author: ${author}\n" +
                             "📝 Commit: ${commitMsg}\n" +
                             "📦 Branch: $branch"
                   mattermostSend (
                       color: 'good',
                       message: msg,
                       endpoint: "${mattermost_url}",
                       channel: "${mattermost_channel}"
                   )
               }
           }
       }
       failure {
           script {
               dir('./be/judge') {
                   def author = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                   def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                   def msg = "🖥️ [Back-end: Judge Server]\n" +
                             "❌ [Build Failure] <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>\n" +
                             "👨‍💻 Author: ${author}\n" +
                             "📝 Commit: ${commitMsg}\n" +
                             "📦 Branch: $branch"
                   mattermostSend (
                       color: 'danger',
                       message: msg,
                       endpoint: "${mattermost_url}",
                       channel: "${mattermost_channel}"
                   )
               }
           }
       }
   }
}
