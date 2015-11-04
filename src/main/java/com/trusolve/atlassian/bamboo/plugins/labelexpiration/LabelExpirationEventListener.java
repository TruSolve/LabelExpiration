package com.trusolve.atlassian.bamboo.plugins.labelexpiration;

import com.atlassian.bamboo.agent.AgentType;
import com.atlassian.bamboo.buildqueue.manager.AgentManager;
import com.atlassian.bamboo.deployments.execution.events.DeploymentFinishedEvent;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.deployments.results.service.DeploymentResultService;
import com.atlassian.bamboo.event.BuildCompletedEvent;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.BuildAgent;
import com.atlassian.bamboo.v2.build.events.PostBuildCompletedEvent;
import com.atlassian.event.api.EventListener;

public class LabelExpirationEventListener
	extends LabelExpirationCore
{
	@EventListener
	public void handleDeploymentFinished(DeploymentFinishedEvent deploymentFinishedEvent)
	{
		DeploymentResult deploymentResult = deploymentResultService.getDeploymentResult(deploymentFinishedEvent.getDeploymentResultId());
		if( ! AgentType.LOCAL.equals(deploymentResult.getAgent().getType()) )
		{
			// The deployment was run on a remote agent, we need to execute the post processing.
			for(TaskDefinition taskDefinition : deploymentResult.getEnvironment().getTaskDefinitions())
			{
				if( "com.trusolve.atlassian.bamboo.plugins.LabelExpiration:labelExpirationTask".equals(taskDefinition.getPluginKey()))
				{
					System.out.println(taskDefinition.toString());
				}
			}
		}
	}
	
	@EventListener
	public void handlePostBuildCompleted(PostBuildCompletedEvent postBuildCompletedEvent)
		throws Exception
	{
		ResultsSummary resultsSummary = resultsSummaryManager.getResultsSummary(postBuildCompletedEvent.getPlanResultKey());
		BuildAgent buildAgent = agentManager.getAgent(resultsSummary.getBuildAgentId());
		if( ! AgentType.LOCAL.equals(buildAgent.getType()) )
		{
			// The deployment was run on a remote agent, we need to execute the post processing.
			for(TaskDefinition taskDefinition : postBuildCompletedEvent.getContext().getTaskDefinitions() )
			{
				if( "com.trusolve.atlassian.bamboo.plugins.LabelExpiration:labelExpirationTask".equals(taskDefinition.getPluginKey()))
				{
					this.performLabel(postBuildCompletedEvent.getContext().getParentBuildContext().getPlanResultKey(), taskDefinition.getConfiguration(), 0);
				}
			}
		}
	}


}
