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

        stage('Checkout external proj') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/dariancampospeya/test.git'
    
                sh "ls -lat"

            }
            File sourceFile = new File("./step-dev.groovy");
            Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
            GroovyObject myObject = (GroovyObject) groovyClass.newInstance()

        }
    }

}
