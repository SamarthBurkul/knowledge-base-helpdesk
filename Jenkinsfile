// Knowledge Base Helpdesk - CI pipeline (Windows Jenkins agent with Docker Desktop).
//
// Flow: checkout -> compile -> unit + integration tests -> package JAR
//       -> start app -> Selenium (Chrome in Docker) -> versioned Docker image
//       -> temporary container deployment -> health gate -> (optional) Docker Hub.
//
// Any failing test fails its `bat` step, which stops the pipeline before the
// Docker image is built or deployed. JUnit reports are published in `post`.
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
        APP_CONTAINER = "helpdesk-app-ci-${BUILD_NUMBER}"
        SELENIUM_IMAGE = 'selenium/standalone-chrome:latest'
        SELENIUM_CONTAINER = "selenium-chrome-ci-${BUILD_NUMBER}"
        SELENIUM_HOST_PORT = '24444'
        SELENIUM_REMOTE_URL = 'http://localhost:24444'
        SELENIUM_BASE_URL = 'http://host.docker.internal:18082'
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
                bat '''
                    java -version
                    mvn -v
                    docker version --format "Docker engine {{.Server.Version}}"
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

        stage('Start application for Selenium') {
            when { expression { params.RUN_SELENIUM } }
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    # Run a copy so Windows does not lock target\\*.jar.
                    New-Item -ItemType Directory -Force -Path ci-app | Out-Null
                    Copy-Item target\\knowledge-base-helpdesk-1.0.0.jar ci-app\\app-under-test.jar -Force

                    $logPath = Join-Path $env:WORKSPACE 'app.log'
                    $errorPath = Join-Path $env:WORKSPACE 'app-error.log'
                    $process = Start-Process `
                        -FilePath 'java' `
                        -ArgumentList @('-jar', 'ci-app\\app-under-test.jar', "--server.port=$env:APP_PORT") `
                        -WorkingDirectory $env:WORKSPACE `
                        -RedirectStandardOutput $logPath `
                        -RedirectStandardError $errorPath `
                        -PassThru
                    Set-Content -Path (Join-Path $env:WORKSPACE 'app.pid') -Value $process.Id

                    $ready = $false
                    for ($attempt = 0; $attempt -lt 45; $attempt++) {
                        if ($process.HasExited) {
                            Get-Content $logPath, $errorPath -ErrorAction SilentlyContinue
                            throw "Application process exited with code $($process.ExitCode)."
                        }
                        try {
                            $health = Invoke-RestMethod -Uri "$env:APP_BASE_URL/actuator/health" -TimeoutSec 3
                            if ($health.status -eq 'UP') { $ready = $true; break }
                        } catch { }
                        Start-Sleep -Seconds 2
                    }
                    if (-not $ready) {
                        Get-Content $logPath, $errorPath -ErrorAction SilentlyContinue
                        throw "Application did not report UP at $env:APP_BASE_URL/actuator/health."
                    }
                    Write-Output "Application under test is UP on port $env:APP_PORT"
                '''
            }
        }

        stage('Selenium tests') {
            when { expression { params.RUN_SELENIUM } }
            steps {
                bat '''
                    docker run --detach ^
                      --name "%SELENIUM_CONTAINER%" ^
                      --publish "%SELENIUM_HOST_PORT%:4444" ^
                      --shm-size=2g ^
                      "%SELENIUM_IMAGE%"
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
                        docker logs $env:SELENIUM_CONTAINER
                        throw "Selenium container did not become ready at $statusUrl."
                    }
                    Write-Output 'Selenium Grid is ready'
                '''
                bat '''
                    mvn -B ^
                      "-Dselenium.enabled=true" ^
                      "-Dselenium.remoteUrl=%SELENIUM_REMOTE_URL%" ^
                      "-DbaseUrl=%SELENIUM_BASE_URL%" ^
                      -Dtest=HelpdeskSeleniumTest test
                '''
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

        stage('Temporary-container deployment') {
            steps {
                bat '''
                    docker run --detach ^
                      --name "%APP_CONTAINER%" ^
                      --publish "%DEPLOY_PORT%:8080" ^
                      "%APP_IMAGE%"
                '''
            }
        }

        stage('Deployment health check') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $healthUrl = "http://localhost:$env:DEPLOY_PORT/actuator/health"
                    $ready = $false
                    for ($attempt = 0; $attempt -lt 45; $attempt++) {
                        $dockerHealth = docker inspect $env:APP_CONTAINER --format '{{.State.Health.Status}}'
                        try {
                            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
                            if ($health.status -eq 'UP' -and $dockerHealth -eq 'healthy') {
                                Write-Output "HTTP health: UP, Docker health: $dockerHealth"
                                $ready = $true
                                break
                            }
                        } catch { }
                        Start-Sleep -Seconds 2
                    }
                    cmd /c "docker logs $env:APP_CONTAINER > deploy-container.log 2>&1"
                    if (-not $ready) {
                        Get-Content deploy-container.log -Tail 50
                        throw "Deployed container did not become healthy (last Docker health: $dockerHealth)."
                    }
                '''
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
                                $env:DOCKERHUB_TOKEN | docker login `
                                    --username $env:DOCKERHUB_USER `
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
            script {
                try {
                    powershell '''
                        $ErrorActionPreference = 'Continue'
                        $pidFile = Join-Path $env:WORKSPACE 'app.pid'
                        if (Test-Path $pidFile) {
                            $appPid = [int](Get-Content $pidFile -Raw)
                            $process = Get-Process -Id $appPid -ErrorAction SilentlyContinue
                            if ($null -ne $process) {
                                Stop-Process -Id $appPid -Force
                            }
                            Remove-Item $pidFile -Force
                        }
                    '''
                } catch (Exception cleanupError) {
                    echo "Could not clean up the test application: ${cleanupError}"
                }

                bat(returnStatus: true, script: '''
                    docker container inspect "%SELENIUM_CONTAINER%" >NUL 2>&1
                    if not errorlevel 1 docker rm --force "%SELENIUM_CONTAINER%"
                    docker container inspect "%APP_CONTAINER%" >NUL 2>&1
                    if not errorlevel 1 docker rm --force "%APP_CONTAINER%"
                    exit /b 0
                ''')
            }

            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
            archiveArtifacts allowEmptyArchive: true,
                    artifacts: 'target/surefire-reports/**,test-output/**,app.log,app-error.log,deploy-container.log'
        }
        success {
            echo "Pipeline succeeded. Image ${env.APP_IMAGE} passed tests and the deployment health gate."
        }
        failure {
            echo 'Pipeline failed. Later stages (image build, deployment) do not run after a failed stage.'
        }
    }
}
