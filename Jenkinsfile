pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('Build') {
            steps {
                sh "mvn -Prelease clean install"
            }
        }
        stage('Test') {
            steps {
                sh "mvn -Ptest test"
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Image') {
            when {
                 branch 'elza-1.3'
             }
            steps {
                sh "cp distrib/elza-war/target/elza-*.war distrib/elza-docker/elza.war"
                sh "$HOME/docker-buildtag.sh elza distrib/elza-docker"
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
