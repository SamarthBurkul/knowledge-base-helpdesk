pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'PUBLISH_TO_DOCKERHUB',
            defaultValue: false,
            description: 'Push the validated build image to Docker Hub.'
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
    }

    environment {
        APP_PORT = '18082'
        APP_BASE_URL = 'http://localhost:18082'
        DEPLOY_PORT = '18083'
        APP_IMAGE = "knowledge-base-helpdesk:${BUILD_NUMBER}"
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
            }
        }

        stage('Maven build') {
            steps {
                bat 'mvn -B -DskipTests package'
            }
        }

        stage('Unit tests') {
            steps {
                bat 'mvn -B -Dtest=ArticleServiceTest test'
            }
        }

        stage('Start application for Selenium') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $logPath = Join-Path $env:WORKSPACE 'app.log'
                    $errorPath = Join-Path $env:WORKSPACE 'app-error.log'
                    $process = Start-Process `
                        -FilePath 'java' `
                        -ArgumentList @(
                            '-jar',
                            'target/knowledge-base-helpdesk-1.0.0.jar',
                            "--server.port=$env:APP_PORT"
                        ) `
                        -WorkingDirectory $env:WORKSPACE `
                        -RedirectStandardOutput $logPath `
                        -RedirectStandardError $errorPath `
                        -PassThru
                    Set-Content -Path (Join-Path $env:WORKSPACE 'app.pid') `
                        -Value $process.Id

                    $ready = $false
                    for ($attempt = 0; $attempt -lt 30; $attempt++) {
                        if ($process.HasExited) {
                            Get-Content $logPath, $errorPath -ErrorAction SilentlyContinue
                            throw "Application process exited with code $($process.ExitCode)."
                        }
                        try {
                            $health = Invoke-RestMethod `
                                -Uri "$env:APP_BASE_URL/actuator/health" `
                                -TimeoutSec 3
                            if ($health.status -eq 'UP') {
                                $ready = $true
                                break
                            }
                        } catch {
                            Start-Sleep -Seconds 2
                        }
                    }

                    if (-not $ready) {
                        Get-Content $logPath, $errorPath -ErrorAction SilentlyContinue
                        throw "Application did not report UP at $env:APP_BASE_URL/actuator/health."
                    }
                '''
            }
        }

        stage('Selenium tests') {
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
                            if ($status.value.ready -eq $true) {
                                $status | ConvertTo-Json -Compress
                                $ready = $true
                                break
                            }
                        } catch {
                            Start-Sleep -Seconds 2
                        }
                    }

                    if (-not $ready) {
                        docker logs $env:SELENIUM_CONTAINER
                        throw "Selenium container did not become ready at $statusUrl."
                    }
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

        stage('Full test suite with Selenium') {
            steps {
                bat '''
                    mvn -B ^
                      "-Dselenium.enabled=true" ^
                      "-Dselenium.remoteUrl=%SELENIUM_REMOTE_URL%" ^
                      "-DbaseUrl=%SELENIUM_BASE_URL%" ^
                      test
                '''
            }
        }

        stage('Docker image build') {
            steps {
                bat 'docker build --tag "%APP_IMAGE%" .'
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

        stage('Health check') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $healthUrl = "http://localhost:$env:DEPLOY_PORT/actuator/health"
                    $ready = $false

                    for ($attempt = 0; $attempt -lt 30; $attempt++) {
                        try {
                            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
                            if ($health.status -eq 'UP') {
                                $health | ConvertTo-Json -Compress
                                $ready = $true
                                break
                            }
                        } catch {
                            Start-Sleep -Seconds 2
                        }
                    }

                    if (-not $ready) {
                        docker logs $env:APP_CONTAINER
                        throw "Deployed application did not report UP at $healthUrl."
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
                    currentBuild.result = 'FAILURE'
                }

                try {
                    bat '''
                        docker container inspect "%SELENIUM_CONTAINER%" >NUL 2>&1
                        if not errorlevel 1 docker rm --force "%SELENIUM_CONTAINER%"
                        exit /b 0
                    '''
                } catch (Exception cleanupError) {
                    echo "Could not clean up the Selenium container: ${cleanupError}"
                    currentBuild.result = 'FAILURE'
                }

                try {
                    bat '''
                        docker container inspect "%APP_CONTAINER%" >NUL 2>&1
                        if not errorlevel 1 docker rm --force "%APP_CONTAINER%"
                        exit /b 0
                    '''
                } catch (Exception cleanupError) {
                    echo "Could not clean up the temporary container: ${cleanupError}"
                    currentBuild.result = 'FAILURE'
                }
            }

            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
            archiveArtifacts allowEmptyArchive: true,
                    artifacts: 'target/surefire-reports/**,test-output/**,app.log,app-error.log'
        }
    }
}
