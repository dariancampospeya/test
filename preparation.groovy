pipeline {
    agent { label "master"}
   
    //add environment
    environment {
       GITHUBREPO = ''
    }

    stages {
        //inject stage code
        stage('Preparation') {
                    steps {
                       sh 'curl -OL https://raw.githubusercontent.com/dariancampospeya/test/master/extravars.properties'
            
                            script {
                                def props = readProperties file: 'extravars.properties'
                                env.nombre = props.nombre
                                env.apellido = props.apellido
                                env.edad = props.edad
                            }
            
                        sh "echo My name is $nombre"
                        sh "echo My lastname is $apellido"
                        sh "echo My year is $edad"  
            }
        }

        //use your code stage pipeline
        stage('develop'){
            steps{
                sh "echo write your code"
            }
        }
    }

    node {
        git url: 'https://github.com/jglick/simple-maven-project-with-tests.git'
        def mvnHome = tool 'M3'
        env.PATH = "${mvnHome}/bin:${env.PATH}"
        sh 'mvn -B verify'
    }

}
