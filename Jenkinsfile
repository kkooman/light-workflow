pipeline {
  agent any

  environment {
    TOMCAT_HOME = '/opt/tomcat'
    APP_CONTEXT = 'ROOT'
    STATUS_URL = 'http://127.0.0.1:8080/status.html'
    LB_STATUS_URL = 'http://127.0.0.1:8080/status.html'
    MAINTENANCE_FILE = '/tmp/lightworkflow-maintenance.marker'
    DELAY_SECONDS = '60'
    WAR_PATH = '/workspace/build/libs/app.war'
  }

  stages {
    stage('Build') {
      steps {
        sh './gradlew clean build -x test'
      }
    }

    stage('Zero Downtime Deploy') {
      steps {
        sh '''
          set -euxo pipefail
          chmod +x ./deploy/zero-downtime-tomcat.sh
          WAR_PATH="${WAR_PATH}" \
          TOMCAT_HOME="${TOMCAT_HOME}" \
          APP_CONTEXT="${APP_CONTEXT}" \
          STATUS_URL="${STATUS_URL}" \
          LB_STATUS_URL="${LB_STATUS_URL}" \
          MAINTENANCE_FILE="${MAINTENANCE_FILE}" \
          DELAY_SECONDS="${DELAY_SECONDS}" \
          ./deploy/zero-downtime-tomcat.sh
        '''
      }
    }
  }
}
