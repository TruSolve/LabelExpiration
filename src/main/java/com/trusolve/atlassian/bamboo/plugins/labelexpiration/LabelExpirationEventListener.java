package com.trusolve.atlassian.bamboo.plugins.labelexpiration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.agent.AgentType;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.deployments.execution.events.DeploymentFinishedEvent;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.BuildAgent;
import com.atlassian.bamboo.v2.build.events.PostBuildCompletedEvent;
import com.atlassian.bamboo.variable.VariableContextSnapshot;
import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.trusolve.atlassian.bamboo.plugins.labelexpiration.tasks.LabelExpirationTaskConfigurator;

public class LabelExpirationEventListener
	extends LabelExpirationCore
{
	private static final Logger log = LoggerFactory.getLogger(LabelExpirationEventListener.class);
	private static final String PLUGIN_KEY = "com.trusolve.atlassian.bamboo.plugins.LabelExpiration:labelExpirationTask";
	
	private class Holder<T>
	{
		private T value = null;
		public void set(T value)
		{
			this.value = value; 
		}
		
		public T get()
		{
			return this.value;
		}
	}
	
	
	@EventListener
	public void handleDeploymentFinished(DeploymentFinishedEvent deploymentFinishedEvent)
		throws Exception
	{
		DeploymentResult deploymentResult = deploymentResultService.getDeploymentResult(deploymentFinishedEvent.getDeploymentResultId());

		log.debug("Performing DeploymentFinishedEvent processing for {} in LabelExpiration plugin.", deploymentResult.getKey() );
		for( PlanResultKey planResultKey : deploymentVersionService.getRelatedPlanResultKeys(deploymentResult.getDeploymentVersion().getId()) )
		{
			handleLabeling(deploymentResult.getCustomData(), planResultKey, deploymentResult.getDeploymentState());
		}
	}
	
	@EventListener
	public void handlePostBuildCompleted(final PostBuildCompletedEvent postBuildCompletedEvent)
		throws Exception
	{
		final Map<String,String> customBuildData = new HashMap<String,String>();
		final Holder<BuildState> buildState = new Holder<BuildState>();
		transactionTemplate.execute(new TransactionCallback<Object>()
		{
			/* (non-Javadoc)
			 * @see com.atlassian.sal.api.transaction.TransactionCallback#doInTransaction()
			 */
			@Override
			public Object doInTransaction()
			{
				ResultsSummary resultsSummary = resultsSummaryManager.getResultsSummary(postBuildCompletedEvent.getPlanResultKey() );
				
				customBuildData.putAll(resultsSummary.getCustomBuildData());
				buildState.set(resultsSummary.getBuildState());
				return null;
			}
		});

		handleLabeling(customBuildData, postBuildCompletedEvent.getContext().getParentBuildContext().getPlanResultKey(), buildState.get());
	}
	
	private void handleLabeling( Map<String,String> customBuildData, PlanResultKey prk, BuildState buildState )
	{
		final Map<String,Map<String,String>> confs = new HashMap<String,Map<String,String>>();
		
		for( Map.Entry<String, String> e : customBuildData.entrySet() )
		{
			String key = e.getKey();
			if( key != null && key.startsWith(LabelExpirationTaskConfigurator.LABELEXPIRATION_RESULTVARIABLEPREFIX))
			{
				String[] keyVals = key.split("\\.");
				if( ! confs.containsKey(keyVals[1]) )
				{
					confs.put(keyVals[1], new HashMap<String,String>());
				}
				Map<String,String> conf = confs.get(keyVals[1]);
				conf.put(keyVals[2], e.getValue());
			}
		}
		if( confs.size() == 0 )
		{
			log.debug("No configuration found.  Exiting without labeling.");
			return;
		}
		log.debug("Performing PostBuildCompletedEvent processing for {} in LabelExpiration plugin.", prk );
		int processedLabelExpirations = 0;

		for( Map<String,String> conf : confs.values() )
		{
			log.debug("Labeling plan {}", prk);
			this.performLabel(prk, conf, 0, buildState);
			processedLabelExpirations++;
		}
		log.debug("Processed {} Label Expiration directives in {}.", processedLabelExpirations, prk );
	}
}
