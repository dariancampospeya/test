pipeline {
    agent { label "master"}
   
    //add environment
    environment {
       GITHUBREPO = ''
    }

    stages {
        //inject stage code
        {% include 'https://raw.githubusercontent.com/dariancampospeya/test/master/_preparation.groovy' with context %}

        //use your code stage pipeline
        stage('develop'){
            steps{
                sh "echo write your code"
            }
        }
    }

}
