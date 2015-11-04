package com.trusolve.atlassian.bamboo.plugins.labelexpiration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.agent.AgentType;
import com.atlassian.bamboo.deployments.execution.events.DeploymentFinishedEvent;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.BuildAgent;
import com.atlassian.bamboo.v2.build.events.PostBuildCompletedEvent;
import com.atlassian.event.api.EventListener;

public class LabelExpirationEventListener
	extends LabelExpirationCore
{
	private static final Logger log = LoggerFactory.getLogger(LabelExpirationEventListener.class);
	private static final String PLUGIN_KEY = "com.trusolve.atlassian.bamboo.plugins.LabelExpiration:labelExpirationTask";
	
	@EventListener
	public void handleDeploymentFinished(DeploymentFinishedEvent deploymentFinishedEvent)
		throws Exception
	{
		DeploymentResult deploymentResult = deploymentResultService.getDeploymentResult(deploymentFinishedEvent.getDeploymentResultId());
		log.debug("Performing DeploymentFinishedEvent processing for {} in LabelExpiration plugin.", deploymentResult.getKey() );
		int processedLabelExpirations = 0;
		if( ! AgentType.LOCAL.equals(deploymentResult.getAgent().getType()) )
		{
			// The deployment was run on a remote agent, we need to execute the post processing.
			for(TaskDefinition taskDefinition : deploymentResult.getEnvironment().getTaskDefinitions())
			{
				if( PLUGIN_KEY.equals(taskDefinition.getPluginKey()))
				{
					for( PlanResultKey planResultKey : deploymentVersionService.getRelatedPlanResultKeys(deploymentResult.getDeploymentVersion().getId()) )
					{
						log.debug("Labeling plan {}", planResultKey);
						this.performLabel(planResultKey, taskDefinition.getConfiguration(), 0);
					}
					processedLabelExpirations++;
				}
			}
		}
		log.debug("Processed {} Label Expiration directives in {}.", processedLabelExpirations, deploymentResult.getKey() );
	}
	
	@EventListener
	public void handlePostBuildCompleted(PostBuildCompletedEvent postBuildCompletedEvent)
		throws Exception
	{
		ResultsSummary resultsSummary = resultsSummaryManager.getResultsSummary(postBuildCompletedEvent.getPlanResultKey());
		log.debug("Performing PostBuildCompletedEvent processing for {} in LabelExpiration plugin.", postBuildCompletedEvent.getPlanResultKey() );
		BuildAgent buildAgent = agentManager.getAgent(resultsSummary.getBuildAgentId());
		int processedLabelExpirations = 0;
		if( ! AgentType.LOCAL.equals(buildAgent.getType()) )
		{
			// The deployment was run on a remote agent, we need to execute the post processing.
			for(TaskDefinition taskDefinition : postBuildCompletedEvent.getContext().getTaskDefinitions() )
			{
				if( PLUGIN_KEY.equals(taskDefinition.getPluginKey()))
				{
					PlanResultKey prk = postBuildCompletedEvent.getContext().getParentBuildContext().getPlanResultKey();
					log.debug("Labeling plan {}", prk);
					this.performLabel(prk, taskDefinition.getConfiguration(), 0);
					processedLabelExpirations++;
				}
			}
		}
		log.debug("Processed {} Label Expiration directives in {}.", processedLabelExpirations, postBuildCompletedEvent.getPlanResultKey().toString() );
	}


}
