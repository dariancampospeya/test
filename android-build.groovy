#!/usr/bin/env groovy
pipeline {
    agent any
    environment {
        def JAVA_HOME = "${env.JAVA_8_HOME}"
        def PEYA_BASE = "${env.PEYA_BASE}"
        def appname = "android-tracking"
        def env_name = "staging"
    }
    stages {
        stage ("deletedir") {
            steps {
                deleteDir()
            }
        }
        stage("checkout") {
            steps {
                checkout scm
            }
        }
        stage("replace keys") {
            steps {
                sh ''' python $PEYA_BASE/ops-scripts/match.py gradle.properties ${appname} ${env_name} '''
            }
        }
        stage("Build") {
            steps {
                sh '''  ./gradlew :tracking:clean
                        ./gradlew :tracking:assembleRelease
                '''
            }
        }
        stage("SonarQube"){
             steps {
                 sh "./gradlew :tracking:sonarqube"
            }
        }
        stage("Publish"){
            steps {
                sh "./gradlew :tracking:publish"
            }
        }
    }
post{
        failure {
             mail bcc: '', body: "<b>Jenkins Execution</b><br>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br> URL de build: ${env.BUILD_URL}", cc: '', charset: 'UTF-8', from: '', mimeType: 'text/html', replyTo: '', subject: "ERROR CI: Project name -> ${env.JOB_NAME}", to: "mark.vanderouw@pedidosya.com";  
        }
    }
}