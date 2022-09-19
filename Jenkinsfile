pipeline {
    agent any
  stages {
        stage('Build'){  
            steps {
                echo "Building the application"
            }
        }
       stage('Testing') {
            steps {
                sh "npm i"
                sh "npx cypress run --browser firefox"
            }
        }
  }
}
