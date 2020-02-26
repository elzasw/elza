pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    environment {
        JAVA_HOME = "/opt/jdk-11.0.4+11"
        MAVEN_HOME = "/opt/apache-maven-3.6.2"
    }

    stages {
        stage('Build') {
            steps {
                sh "cd elza && $MAVEN_HOME/bin/mvn -U -Prelease,skiptest clean install"
            }
        }
        stage('Test') {
            steps {
                sh "cd elza && $MAVEN_HOME/bin/mvn -Ptest test || echo failed"
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Image') {
            steps {
                sh "cd elza && cp distrib/elza-war/target/elza-*.war distrib/elza-docker/elza.war"
                sh "cd elza && $HOME/docker-buildtag.sh elza distrib/elza-docker"
            }
        }
    }
    post {
        success {
            cleanWs deleteDirs: true, patterns: [[pattern:'**/.git/**',type:'EXCLUDE']]
        }
        failure {
            script { emailext(body:'${DEFAULT_CONTENT}',recipientProviders:[[$class:'CulpritsRecipientProvider']],subject:'${DEFAULT_SUBJECT}') }
            cleanWs deleteDirs: true, patterns: [[pattern:'**/.git/**',type:'EXCLUDE']]
        }
    }
}
