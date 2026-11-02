// Knowledge Base Helpdesk - CI pipeline (Windows Jenkins agent with Docker Desktop).
//
// Flow: checkout -> compile -> unit + integration tests -> package JAR
//       -> versioned Docker image -> temporary test container -> health gate
//       -> Selenium (Chrome in Docker) -> temporary container deployment
//       -> health gate -> (optional) Docker Hub.
//
// The application under test runs in a container, so no step has to keep a
// background process alive on the agent. Any failing test fails its `bat` step,
// which stops the pipeline before the deployment stages. Every container is
// removed in `post`, and JUnit reports are published there too.
pipeline {
    agent any

    tools {
        // Names configured under Manage Jenkins > Tools.
        maven 'Maven3.10'
        jdk 'JDK25'
    }

    parameters {
        booleanParam(
            name: 'RUN_SELENIUM',
            defaultValue: true,
            description: 'Run the Selenium journeys in a selenium/standalone-chrome container.'
        )
        booleanParam(
            name: 'PUBLISH_TO_DOCKERHUB',
            defaultValue: false,
            description: 'Push the validated build image to Docker Hub (needs credential ID "dockerhub").'
        )
        string(
            name: 'DOCKERHUB_REPOSITORY',
            defaultValue: '',
            description: 'Docker Hub namespace/repository (lowercase, no tag).'
        )
    }

    options {
        skipDefaultCheckout(true)
        // The host ports below are fixed, so two builds cannot run at once.
        disableConcurrentBuilds()
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    environment {
        APP_PORT = '18082'
        APP_BASE_URL = 'http://localhost:18082'
        DEPLOY_PORT = '18083'
        APP_IMAGE = "knowledge-base-helpdesk:ci-${BUILD_NUMBER}"
        TEST_APP_CONTAINER = "helpdesk-app-under-test-${BUILD_NUMBER}"
        DEPLOY_CONTAINER = "helpdesk-deploy-ci-${BUILD_NUMBER}"
        SELENIUM_IMAGE = 'selenium/standalone-chrome:latest'
        SELENIUM_CONTAINER = "selenium-chrome-ci-${BUILD_NUMBER}"
        SELENIUM_HOST_PORT = '24444'
        SELENIUM_REMOTE_URL = 'http://localhost:24444'
        SELENIUM_BASE_URL = 'http://host.docker.internal:18082'
        EXPECTED_SELENIUM_TESTS = '6'
        // Marks containers owned by this pipeline. Nothing else carries it, so
        // helpdesk-demo and helpdesk-ansible are never touched.
        CI_CONTAINER_LABEL = 'com.knowledgebase.helpdesk.ci=true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                bat 'git log -1 --format="Building %%h %%s"'
            }
        }

        stage('Tool check') {
            steps {
                // mvn is a .cmd file: without `call`, the batch script ends there.
                bat '''
                    java -version || exit /b 1
                    call mvn -v || exit /b 1
                    docker version --format "Docker engine {{.Server.Version}}"
                    if errorlevel 1 (
                        echo ERROR: Docker engine is not reachable. Is Docker Desktop running?
                        exit /b 1
                    )
                '''
                // A killed build skips `post`; free the fixed ports it left behind.
                powershell '''
                    # 'Continue' so docker's stderr cannot end the script before the
                    # exit-code checks below report the failure.
                    $ErrorActionPreference = 'Continue'

                    $label = $env:CI_CONTAINER_LABEL

                    if ([string]::IsNullOrWhiteSpace($label)) {
                        throw "CI_CONTAINER_LABEL is not configured."
                    }

                    $containers = @(docker ps --all --quiet --filter "label=$label")

                    if ($LASTEXITCODE -ne 0) {
                        throw "Could not inspect leftover Jenkins containers."
                    }

                    foreach ($container in $containers) {
                        $container = "$container".Trim()

                        if (-not [string]::IsNullOrWhiteSpace($container)) {
                            Write-Output "Removing leftover CI container $container"
                            docker rm --force $container

                            if ($LASTEXITCODE -ne 0) {
                                throw "Could not remove CI container $container"
                            }
                        }
                    }

                    Write-Output "CI container cleanup completed."
                '''
            }
        }

        stage('Compile') {
            steps {
                bat 'mvn -B clean compile'
            }
        }

        stage('Unit and integration tests') {
            steps {
                // Selenium journeys are skipped here (selenium.enabled=false)
                // and run for real in the Selenium stage below.
                bat 'mvn -B test'
            }
        }

        stage('Package JAR') {
            steps {
                bat 'mvn -B -DskipTests package'
                archiveArtifacts artifacts: 'target/knowledge-base-helpdesk-*.jar', fingerprint: true
            }
        }

        stage('Docker image build') {
            steps {
                bat '''
                    docker build --tag "%APP_IMAGE%" .
                    docker image inspect "%APP_IMAGE%" --format "Built {{.RepoTags}} {{.Id}}"
                '''
            }
        }

        stage('Start test application container') {
            when { expression { params.RUN_SELENIUM } }
            options { timeout(time: 5, unit: 'MINUTES') }
            steps {
                bat '''
                    docker run --detach ^
                      --name "%TEST_APP_CONTAINER%" ^
                      --label "%CI_CONTAINER_LABEL%" ^
                      --publish "%APP_PORT%:8080" ^
                      "%APP_IMAGE%"
                    if errorlevel 1 (
                        echo ERROR: Could not start %TEST_APP_CONTAINER%. Is port %APP_PORT% already in use?
                        exit /b 1
                    )
                '''
            }
        }

        stage('Wait for test application health') {
            when { expression { params.RUN_SELENIUM } }
            options { timeout(time: 5, unit: 'MINUTES') }
            steps {
                powershell '& .\\jenkins\\Wait-ContainerHealthy.ps1 -ContainerName $env:TEST_APP_CONTAINER -HealthUrl "$env:APP_BASE_URL/actuator/health"'
            }
        }

        stage('Start Selenium Chrome container') {
            when { expression { params.RUN_SELENIUM } }
            options { timeout(time: 10, unit: 'MINUTES') }
            steps {
                bat '''
                    docker run --detach ^
                      --name "%SELENIUM_CONTAINER%" ^
                      --label "%CI_CONTAINER_LABEL%" ^
                      --publish "%SELENIUM_HOST_PORT%:4444" ^
                      --shm-size=2g ^
                      "%SELENIUM_IMAGE%"
                    if errorlevel 1 (
                        echo ERROR: Could not start %SELENIUM_CONTAINER%. Is port %SELENIUM_HOST_PORT% already in use?
                        exit /b 1
                    )
                '''
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $statusUrl = "$env:SELENIUM_REMOTE_URL/status"
                    $ready = $false
                    for ($attempt = 0; $attempt -lt 60; $attempt++) {
                        try {
                            $status = Invoke-RestMethod -Uri $statusUrl -TimeoutSec 3
                            if ($status.value.ready -eq $true) { $ready = $true; break }
                        } catch { }
                        Start-Sleep -Seconds 2
                    }
                    if (-not $ready) {
                        cmd /c "docker logs --tail 50 $env:SELENIUM_CONTAINER 2>&1"
                        throw "Selenium container did not become ready at $statusUrl."
                    }
                    Write-Output 'Selenium Grid is ready'
                '''
                // Prove the route the browser will use before any test depends on it.
                bat '''
                    docker exec "%SELENIUM_CONTAINER%" curl --fail --silent --show-error --max-time 10 "%SELENIUM_BASE_URL%/actuator/health"
                    if errorlevel 1 (
                        echo ERROR: The Chrome container cannot reach %SELENIUM_BASE_URL%.
                        exit /b 1
                    )
                '''
            }
        }

        stage('Selenium browser tests') {
            when { expression { params.RUN_SELENIUM } }
            options { timeout(time: 15, unit: 'MINUTES') }
            steps {
                bat '''
                    mvn -B ^
                      "-Dselenium.enabled=true" ^
                      "-Dselenium.remoteUrl=%SELENIUM_REMOTE_URL%" ^
                      "-DbaseUrl=%SELENIUM_BASE_URL%" ^
                      -Dtest=HelpdeskSeleniumTest test
                '''
                // Skipped journeys do not fail Maven, so check that all of them ran.
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $reportPath = 'target\\surefire-reports\\TEST-com.knowledgebase.helpdesk.selenium.HelpdeskSeleniumTest.xml'
                    $suite = ([xml](Get-Content $reportPath -Raw -Encoding UTF8)).testsuite
                    $summary = "tests=$($suite.tests), failures=$($suite.failures), errors=$($suite.errors), skipped=$($suite.skipped)"
                    if ($suite.tests -ne $env:EXPECTED_SELENIUM_TESTS -or $suite.failures -ne '0' -or
                            $suite.errors -ne '0' -or $suite.skipped -ne '0') {
                        throw "Expected $env:EXPECTED_SELENIUM_TESTS Selenium tests to pass, but got: $summary."
                    }
                    Write-Output "All Selenium journeys ran and passed: $summary"
                '''
            }
        }

        stage('Temporary-container deployment') {
            steps {
                bat '''
                    docker run --detach ^
                      --name "%DEPLOY_CONTAINER%" ^
                      --label "%CI_CONTAINER_LABEL%" ^
                      --publish "%DEPLOY_PORT%:8080" ^
                      "%APP_IMAGE%"
                    if errorlevel 1 (
                        echo ERROR: Could not start %DEPLOY_CONTAINER%. Is port %DEPLOY_PORT% already in use?
                        exit /b 1
                    )
                '''
            }
        }

        stage('Deployment health check') {
            options { timeout(time: 5, unit: 'MINUTES') }
            steps {
                powershell '& .\\jenkins\\Wait-ContainerHealthy.ps1 -ContainerName $env:DEPLOY_CONTAINER -HealthUrl "http://localhost:$env:DEPLOY_PORT/actuator/health"'
            }
        }

        stage('Docker Hub publish (opt-in)') {
            when {
                expression { params.PUBLISH_TO_DOCKERHUB }
            }
            steps {
                script {
                    def repository = params.DOCKERHUB_REPOSITORY?.trim()
                    if (!repository || !(repository ==~ /[a-z0-9]+(?:[._-][a-z0-9]+)*(?:\/[a-z0-9]+(?:[._-][a-z0-9]+)*)?/)) {
                        error('Set DOCKERHUB_REPOSITORY to a valid lowercase namespace/repository.')
                    }

                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )]) {
                        try {
                            powershell '''
                                # Jenkins runs this with a UTF-8 encoding that has a BOM, and
                                # Windows PowerShell would put those bytes in front of the token.
                                $utf8NoBom = New-Object System.Text.UTF8Encoding $false
                                [Console]::InputEncoding = $utf8NoBom
                                $OutputEncoding = $utf8NoBom
                                $dockerHubUser = $env:DOCKERHUB_USER.Trim()
                                $env:DOCKERHUB_TOKEN.Trim() | docker login `
                                    --username $dockerHubUser `
                                    --password-stdin
                                if ($LASTEXITCODE -ne 0) {
                                    throw 'Docker Hub login failed.'
                                }
                            '''
                            bat 'docker tag "%APP_IMAGE%" "%DOCKERHUB_REPOSITORY%:%BUILD_NUMBER%"'
                            bat 'docker push "%DOCKERHUB_REPOSITORY%:%BUILD_NUMBER%"'
                            echo "Published ${repository}:${env.BUILD_NUMBER}"
                        } finally {
                            bat(returnStatus: true, script: 'docker logout')
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // Runs after success, failure, a timeout or a manual abort. Each log is
            // saved before its container goes, because `docker rm` discards it.
            bat(returnStatus: true, script: '''
                call :remove_container "%TEST_APP_CONTAINER%" app-under-test.log
                call :remove_container "%SELENIUM_CONTAINER%" selenium-container.log
                call :remove_container "%DEPLOY_CONTAINER%" deploy-container.log
                docker ps --all --filter "label=%CI_CONTAINER_LABEL%" --format "WARNING: not removed: {{.Names}}"
                exit /b 0

                :remove_container
                docker container inspect "%~1" >NUL 2>&1
                if errorlevel 1 exit /b 0
                docker logs "%~1" > "%~2" 2>&1
                docker rm --force "%~1"
                exit /b 0
            ''')

            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
            archiveArtifacts allowEmptyArchive: true,
                    artifacts: 'target/surefire-reports/**,test-output/**,app-under-test.log,selenium-container.log,deploy-container.log'
        }
        success {
            echo "Pipeline succeeded. Image ${env.APP_IMAGE} passed tests and the deployment health gate."
        }
        failure {
            echo 'Pipeline failed. Later stages (deployment, Docker Hub publishing) do not run after a failed stage.'
        }
    }
}
