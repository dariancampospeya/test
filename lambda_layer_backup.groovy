#!/usr/bin/python3
import os
import shutil
import json
import requests
import jenkins
from git import Repo, Actor

githubApiUrl = "https://api.github.com"
githubToken = "%github_token%"
vaultApiUrl = "https://vault.peya.co"
vaultToken = "%vault_token%"
jenkinsApiUrl = "jenkins.peya.app"
jenkinsToken = "%jenkins_token%"
jenkinsCredentials = "chewie"

def github_api_handler(method, uri, body = {}, params = {}):
    headers = {
        "Accept": "application/vnd.github.baptiste-preview+json, application/vnd.github.loki-preview+json",
        "Authorization": "Bearer "+githubToken
    }
    if method=="POST":
        return requests.post(githubApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="GET":
        return requests.get(githubApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="PUT":
        return requests.put(githubApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="PATCH":
        return requests.patch(githubApiUrl + uri, headers = headers, data = json.dumps(body), params = params)

def git_clone(repo, path):
    shutil.rmtree(path, ignore_errors=True)
    Repo.clone_from("https://%s:x-oauth-basic@github.com/%s" %(githubToken,repo), path)

def git_commit_push(path, message):
    repo = Repo(path)
   
    repo.config_writer().set_value("user", "name", "chewiebot").release()
    repo.config_writer().set_value("user", "email", "chewie@pedidosya.com").release()
   
    repo.git.add(".")
    repo.index.commit(message)
    origin = repo.remote(name='origin')
    origin.push()

def vault_api_handler(method, uri, body = {}, params = {}):

    headers = { "Content-Type": "application/json", "X-Vault-Token": vaultToken }
    
    if method=="POST":
        return requests.post(vaultApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="GET":
        return requests.get(vaultApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="PUT":
        return requests.put(vaultApiUrl + uri, headers = headers, data = json.dumps(body), params = params)
    if method=="PATCH":
        return requests.patch(vaultApiUrl + uri, headers = headers, data = json.dumps(body), params = params)


def jenkins_create(name, team, pipelineFiles):
    server = jenkins.Jenkins('https://chewiebot:{TOKEN}@{jenkinsApiUrl}'.format(TOKEN=jenkinsToken,jenkinsApiUrl=jenkinsApiUrl))
    server.create_job(name, '''
    <com.cloudbees.hudson.plugins.folder.Folder plugin="cloudbees-folder@6.11">
<actions/>
<description/>
<properties>
<com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty>
<inheritanceStrategy class="org.jenkinsci.plugins.matrixauth.inheritance.InheritParentStrategy"/>
<permission>hudson.model.Item.Build:pedidosya*{0}</permission>
<permission>hudson.model.Item.Cancel:pedidosya*{0}</permission>
<permission>hudson.model.Item.Read:pedidosya*{0}</permission>
</com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty>
<org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig plugin="pipeline-model-definition@1.5.0">
<dockerLabel/>
<registry plugin="docker-commons@1.16"/>
</org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig>
</properties>
<folderViews class="com.cloudbees.hudson.plugins.folder.views.DefaultFolderViewHolder">
<views>
<hudson.model.AllView>
<owner class="com.cloudbees.hudson.plugins.folder.Folder" reference="../../../.."/>
<name>All</name>
<filterExecutors>false</filterExecutors>
<filterQueue>false</filterQueue>
<properties class="hudson.model.View$PropertyList"/>
</hudson.model.AllView>
</views>
<tabBar class="hudson.views.DefaultViewsTabBar"/>
</folderViews>
<healthMetrics/>
<icon class="com.cloudbees.hudson.plugins.folder.icons.StockFolderIcon"/>
</com.cloudbees.hudson.plugins.folder.Folder>
    '''.format(team))

    for pipeline in pipelineFiles:
        jobName = pipeline.split('/')[-1]   
        jobBranch = 'develop'   

        if jobName.startswith('live'):  
            jobBranch = 'master'    
        if jobName.startswith('stg'):   
            jobBranch = 'staging'   
        server.create_job('{0}/{1}'.format(name, jobName), '''
        <flow-definition plugin="workflow-job@2.36">
<actions>
<org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin="pipeline-model-definition@1.5.0"/>
<org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin="pipeline-model-definition@1.5.0">
<jobProperties/>
<triggers/>
<parameters/>
<options/>
</org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>
</actions>
<description/>
<keepDependencies>false</keepDependencies>
<properties>
<hudson.plugins.jira.JiraProjectProperty plugin="jira@3.0.11"/>
<jenkins.model.BuildDiscarderProperty>
<strategy class="hudson.tasks.LogRotator">
<daysToKeep>-1</daysToKeep>
<numToKeep>5</numToKeep>
<artifactDaysToKeep>-1</artifactDaysToKeep>
<artifactNumToKeep>-1</artifactNumToKeep>
</strategy>
</jenkins.model.BuildDiscarderProperty>
<org.jenkinsci.plugins.workflow.job.properties.DisableConcurrentBuildsJobProperty/>
<org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
<triggers>
<com.cloudbees.jenkins.GitHubPushTrigger plugin="github@1.29.5">
<spec/>
</com.cloudbees.jenkins.GitHubPushTrigger>
</triggers>
</org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
</properties>

<definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.78">
<scm class="hudson.plugins.git.GitSCM" plugin="git@4.1.1">
<configVersion>2</configVersion>
<userRemoteConfigs>
<hudson.plugins.git.UserRemoteConfig>
<url>git@github.com:{0}/{1}</url>
<credentialsId>{2}</credentialsId>
</hudson.plugins.git.UserRemoteConfig>
</userRemoteConfigs>
<branches>
<hudson.plugins.git.BranchSpec>
<name>*/{3}</name>
</hudson.plugins.git.BranchSpec>
</branches>
<doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
<submoduleCfg class="list"/>
<extensions/>
</scm>
<scriptPath>{4}</scriptPath>
<lightweight>true</lightweight>
</definition>


<triggers/>
<disabled>false</disabled>
</flow-definition>
        '''.format('pedidosya', name, jenkinsCredentials, jobBranch, pipeline))


def jenkins_build(repoName, branch, tag):
    server = jenkins.Jenkins('https://chewiebot:{TOKEN}@{jenkinsApiUrl}'.format(TOKEN=jenkinsToken, jenkinsApiUrl=jenkinsApiUrl))

    jobBranch = branch

    if jobBranch.startswith('live'):
        jobBranch = 'master'
    if jobBranch.startswith('stg'):
        jobBranch = 'staging'
    
    job = '{0}/{1}'.format(repoName, jobBranch)

    server.build_job(job, {'tag': tag})

