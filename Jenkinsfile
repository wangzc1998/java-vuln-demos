pipeline {
    agent any
    stages {
        stage('Build Jar') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }
    }
    post {
        success {
            archiveArtifacts artifacts: 'target/java-vuln-demos.jar', fingerprint: true
        }
    }
}
