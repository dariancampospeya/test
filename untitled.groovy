pipeline {
  agent {
    kubernetes {
      yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins: slave
spec:
  volumes: 
  - name: base
    nfs: 
    # URL for the NFS server
      server: peya-base.live.peja.co 
      path: /
  - name: dind-storage
    emptyDir: {}
  containers:
  - name: docker
    image: 598167139681.dkr.ecr.us-east-1.amazonaws.com/jenkins-helper:latest
    command:
    - cat
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    - name: AWS_ACCESS_KEY_ID
      valueFrom:
        secretKeyRef:
          name: aws
          key: access
    - name: AWS_SECRET_ACCESS_KEY
      valueFrom:
        secretKeyRef:
          name: aws
          key: secret
    imagePullPolicy: Always
  - name: dind
    image: docker:19.03.5-dind
    securityContext:
      privileged: true
    env:
    - name: DOCKER_TLS_CERTDIR
      value: ''
    volumeMounts:
    - name: dind-storage
      mountPath: /var/lib/docker
  - name: gradle
   image: gradle:6.2.2-jdk8
   command:
   - cat
"""
    }
  }
  environment {
    def appname = "jarvis-service"
    def region = "us-east-1"
    def PEYA_BASE = "/opt/peya-base"
  }
  stages {

    stage("checkout") {
      steps {
          checkout scm
      }
    }

    stage ('build'){
      steps {
        //replace
          container('python') {
              withCredentials([string(credentialsId: "vault_token", variable: 'token')]) {
                sh ''' python $PEYA_BASE/ops-scripts/match.py gradle.properties ${appname} ${env_name} '''
            }
        }
      }

      steps{
        container('gradle'){
             sh '''
                echo GradleOk
                ./gradlew :module:clean
                ./gradlew :module:assembleRelease
             '''
          }
        }
      }
    }
  }

  containerTemplate(name: 'gradle', image: 'gradle:4.5.1-jdk9', command: 'cat', ttyEnabled: true),
    hostPathVolume(mountPath: '/home/gradle/.gradle', hostPath: '/tmp/jenkins/.gradle'),

