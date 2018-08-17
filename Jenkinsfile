pipeline {
    agent any

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
                 branch 'elza-0.17'
             }
            steps {
                sh "export DOCKER_TLS_VERIFY="1""
                sh "export DOCKER_HOST="tcp://10.0.0.212:2376""
                sh "export DOCKER_CERT_PATH="$HOME/machines/e1""
                sh "cp distrib/elza-war/target/elza-*.war distrib/elza-docker/elza.war"
                sh "docker build -t elza -t docker.marbes.cz/elza:$BRANCH_NAME distrib/elza-docker && docker push docker.marbes.cz/elza:$BRANCH_NAME"
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
