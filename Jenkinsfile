pipeline {
    agent any
  stages {
        stage('Build'){  
            steps {
                echo "Building the application"
            }
        }
        stage('Dependencies') {
          steps {
             sh "npm install"        
      }
    }
  }
}
