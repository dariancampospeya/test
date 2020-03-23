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
    image: brunogf/docker-aws:19.03.5
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
  - name: python
    image: python:2.7.16
    command:
    - cat
    tty: true
    volumeMounts:
    - name: base
      mountPath: /opt/peya-base
  - name: python3
    image: python:3.8
    command:
    - cat
    tty: true
"""
    }
  }
  environment {
    def appname = "jarvis-service"
    def region = "us-east-1"
    def PEYA_BASE = "/opt/peya-base"
  }
  stages {
    stage('deploy') {
      steps {
        container('python') {
          dir("tasks/jarvis-layer/python"){
            withCredentials([string(credentialsId: "vault_token", variable: 'token')]) {
              sh '''
                pip install requests
                python ${PEYA_BASE}/ops-scripts/replace.py jarvis_layer.py ${appname} live ${token}
              '''
            }
          }
        }
        container('python3') {
          dir("tasks/jarvis-layer/python"){
            sh '''
              pip3 install -r requirements.txt -t .
            '''
          }
        }
        container('docker') {
          dir("tasks/jarvis-layer"){
            sh '''  
              zip -r jarvis_layer.zip ./python;
              aws lambda publish-layer-version --layer-name jarvis_layer  \
              --description "Jarvis common functions" --license-info "MIT" \
              --region us-east-1 --compatible-runtimes python3.7\
              --zip-file fileb://jarvis_layer.zip 
            '''
          }
        }
      }
    }
  }
}
