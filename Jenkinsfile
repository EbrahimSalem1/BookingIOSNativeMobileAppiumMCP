// Real-device pipeline for a self-hosted Mac mini farm (USB-attached iPhones).
// Infrastructure requirements are documented in README.md > "Real devices".
pipeline {
    agent { label 'macos && ios-devices' }

    parameters {
        string(name: 'TAGS', defaultValue: '@smoke', description: 'Cucumber tag expression')
        string(name: 'DEVICE_POOL', defaultValue: 'iPhone 14@00008110-XXXXXXXXXXXX@17.5', description: 'name@udid@ios, comma separated')
        string(name: 'APP_IPA', defaultValue: '', description: 'Path/URL of the signed .ipa (development or ad-hoc)')
    }

    environment {
        BOOKING_USERNAME = credentials('booking-qa-username')
        BOOKING_PASSWORD = credentials('booking-qa-password')
        XCODE_ORG_ID     = credentials('apple-team-id')
        BOOKING_APP_PATH = "${params.APP_IPA}"
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
        // One run per physical device set at a time.
        lock(resource: 'ios-device-farm')
    }

    stages {
        stage('Framework checks') {
            steps { sh 'mvn -B -q test -Punit' }
        }
        stage('Appium') {
            steps { sh 'chmod +x scripts/*.sh && ./scripts/start-appium.sh' }
        }
        stage('Run on devices') {
            steps {
                sh """
                  mvn -B test -Denv=ci -Ddevice.real=true \
                    -Ddevice.pool='${params.DEVICE_POOL}' \
                    -Dcucumber.filter.tags='${params.TAGS}' \
                    -Dthreads=\$(echo '${params.DEVICE_POOL}' | tr ',' '\\n' | wc -l | tr -d ' ') \
                    -Dmaven.test.failure.ignore=true
                """
                sh '[ -s target/rerun/failed.txt ] && mvn -B test -Prerun -Denv=ci -Ddevice.real=true -Ddevice.pool="${DEVICE_POOL}" -Dmaven.test.failure.ignore=true || true'
            }
        }
    }

    post {
        always {
            withCredentials([string(credentialsId: 'anthropic-api-key', variable: 'ANTHROPIC_API_KEY')]) {
                sh 'python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results --llm || true'
            }
            allure includeProperties: false, results: [[path: 'target/allure-results']]
            archiveArtifacts artifacts: 'target/ai-failure-analysis.*, target/locator-drift-report.json, target/logs/**, target/appium-server.log', allowEmptyArchive: true
            sh 'kill $(cat target/appium.pid) 2>/dev/null || true'
        }
    }
}
