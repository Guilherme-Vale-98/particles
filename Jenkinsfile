def runMaven(String arguments) {
    if (isUnix()) {
        sh "./mvnw ${arguments}"
    } else {
        bat ".\\mvnw.cmd ${arguments}"
    }
}

def commandSucceeds(String command) {
    if (isUnix()) {
        return sh(script: command, returnStatus: true) == 0
    }

    return bat(script: command, returnStatus: true) == 0
}

pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Environment') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'chmod +x ./mvnw'
                    }

                    if (isUnix()) {
                        sh 'java -version'
                        sh './mvnw -version'
                    } else {
                        bat 'java -version'
                        bat '.\\mvnw.cmd -version'
                    }
                }
            }
        }

        stage('Compile') {
            steps {
                script {
                    runMaven('-DskipTests package')
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    runMaven('"-Dtest=!*IntegrationTests,!*SchemaIntegrationTests,!*FlowIntegrationTests" test')
                }
            }
        }

        stage('Module Boundary Tests') {
            steps {
                script {
                    runMaven('"-Dtest=ApplicationModuleBoundaryTests" test')
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    if (commandSucceeds('docker info')) {
                        runMaven('"-Dtest=*IntegrationTests,*SchemaIntegrationTests,*FlowIntegrationTests" test')
                    } else {
                        echo 'Docker is not available on this Jenkins agent. Skipping Testcontainers integration tests.'
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
        }
    }
}
