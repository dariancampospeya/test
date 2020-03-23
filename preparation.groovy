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

        stage('Checkout external proj') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/dariancampospeya/test.git'
    
                sh "ls -lat"
                sh "groovy step-dev.groovy"

            }
        }
    }

}
