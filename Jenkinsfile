package org.jenkinsci.plugins.github_integration.its.WorkflowITest

def setStatusInGitHub(state,message){
    switch(params.RELEASE){
        case true:
            echo state+" RELEASE"
            break
        case false:
             step([$class: 'GitHubCommitStatusSetter',
                            reposSource: [$class: "ManuallyEnteredRepositorySource", url: env.GIT_URL],
                            contextSource: [$class: 'ManuallyEnteredCommitContextSource',  context: env.GITHUB_CONTEXT],
                            statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult',
                                                                                                    message: message,
                                                                                                    state: state]]]])
    }
}


pipeline {
    agent any
    environment {
        //Please be aware it is jenkins instation specific
        CREDENTIALS_ID = 'git-github'
        GITHUB_CONTEXT = 'xtrf-jenkins'
    }
    parameters {
        booleanParam(name: 'WIPE_OUT', defaultValue: false, description: 'Do you want to wipe out workspace?')
        booleanParam(name: 'RELEASE', defaultValue: false, description: 'You wanna release or just build a thing?')
        booleanParam(name: 'DEPLOY', defaultValue: false, description: 'You wanna deploy a thing?')
        choice(name: 'VERSION', choices: ['PATCH', 'MINOR', 'MAJOR'], description: 'Which version?')
    }
    stages {
        stage('Clean Workspace') {
             when {
                expression {
                    params.WIPE_OUT == true
                }
            }
            steps {
                step([$class: 'WsCleanup'])
            }
        }
        stage('Echo Parameters') {
            steps {
                echo "Hello ${params.RELEASE}"
                echo "Version: ${params.VERSION}"
                sh 'printenv'
            }

        }
        stage("Init Pull Request Verification"){
            when {
                expression {
                    params.RELEASE == false
                }
            }
            steps{
                echo "Setting GitHub Status to Pending"
                setStatusInGitHub("PENDING","Wait for it...")
            }
        }

       stage('Set Branch') {
            steps {
                git branch: env.GIT_BRANCH, url: env.GIT_URL, credentialsId: env.CREDENTIALS_ID

            }
       }

        stage("Build") {
            when {
                expression {
                    params.RELEASE == false
                }
            }
            steps {
                sh "mvn clean install -Pfront"
            }
        }

        stage("Release Patch") {
            when {
                expression {
                    params.RELEASE == true && params.VERSION == "PATCH"
                }
            }
            steps {
                sshagent([env.CREDENTIALS_ID]) {
                    sh "mvn -e -B release:clean -Ddependency.locations.enabled=false"
                    sh "mvn -e -B release:prepare -Ddependency.locations.enabled=false"
                    sh "mvn -e -B release:perform -Ddependency.locations.enabled=false -Pfront"
                }
            }
        }

        stage("Release Minor"){
            when {
                expression {
                    params.RELEASE == true && params.VERSION == "MINOR"
                }
            }
            steps {
                sshagent([env.CREDENTIALS_ID]) {
                    sh "mvn clean"
                    sh "mvn build-helper:parse-version release:branch -B -DbranchName=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}-dev -DreleaseVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.0 -DdevelopmentVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.nextMinorVersion}.0-SNAPSHOT"
                }
            }
        }

        stage("Release Major"){
            when {
                expression {
                    params.RELEASE == true && params.VERSION == "MAJOR"
                }
            }
            steps {
                sshagent([env.CREDENTIALS_ID]) {
                    sh "mvn clean"
                    sh "mvn build-helper:parse-version release:branch -B -DbranchName=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}-dev -DreleaseVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.0 -DdevelopmentVersion=\\\${parsedVersion.nextMajorVersion}.0.0-SNAPSHOT"
                }
            }
        }

        stage("Deploy") {
            when{
                 expression {
                    params.DEPLOY == true
                }
            }
            steps{
                sh "mvn clean deploy -B -U -Pfront"
            }

        }
    }

    post {
        success {
            echo "success"
            setStatusInGitHub("SUCCESS","It's ok")
        }
        unsuccessful {
            echo "unsuccess"
            setStatusInGitHub("FAILURE","Build failed")
        }

    }
}
