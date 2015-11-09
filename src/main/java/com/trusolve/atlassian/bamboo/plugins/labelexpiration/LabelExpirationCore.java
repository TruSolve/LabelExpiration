package com.trusolve.atlassian.bamboo.plugins.labelexpiration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.buildqueue.manager.AgentManager;
import com.atlassian.bamboo.deployments.results.service.DeploymentResultService;
import com.atlassian.bamboo.deployments.versions.service.DeploymentVersionService;
import com.atlassian.bamboo.labels.Label;
import com.atlassian.bamboo.labels.LabelManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryCriteria;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.v2.build.agent.capability.AgentContext;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.trusolve.atlassian.bamboo.plugins.labelexpiration.tasks.LabelExpirationTaskConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class LabelExpirationCore
{
	private static final Logger log = LoggerFactory.getLogger(LabelExpirationCore.class);
	
	//Getter/Setters for Bamboo manager object injection
	protected PlanManager planManager = null;
	public PlanManager getPlanManager()
	{
		return planManager;
	}

	public void setPlanManager(PlanManager planManager)
	{
		this.planManager = planManager;
	}

	protected LabelManager labelManager = null;
	public LabelManager getLabelManager()
	{
		return labelManager;
	}

	public void setLabelManager(LabelManager labelManager)
	{
		this.labelManager = labelManager;
	}

	protected ResultsSummaryManager resultsSummaryManager = null;
	public ResultsSummaryManager getResultsSummaryManager()
	{
		return resultsSummaryManager;
	}

	public void setResultsSummaryManager(ResultsSummaryManager resultsSummaryManager)
	{
		this.resultsSummaryManager = resultsSummaryManager;
	}

	protected TransactionTemplate transactionTemplate = null;
	public TransactionTemplate getTransactionTemplate()
	{
		return transactionTemplate;
	}
	public void setTransactionTemplate(TransactionTemplate transactionTemplate)
	{
		this.transactionTemplate = transactionTemplate;
	}

	protected AgentContext agentContext = null;
	public AgentContext getAgentContext()
	{
		return agentContext;
	}
	public void setAgentContext(AgentContext agentContext)
	{
		this.agentContext = agentContext;
	}

	protected DeploymentResultService deploymentResultService = null;
	public DeploymentResultService getDeploymentResultService()
	{
		return deploymentResultService;
	}
	public void setDeploymentResultService(DeploymentResultService deploymentResultService)
	{
		this.deploymentResultService = deploymentResultService;
	}

	protected AgentManager agentManager = null;
	public AgentManager getAgentManager()
	{
		return agentManager;
	}
	public void setAgentManager(AgentManager agentManager)
	{
		this.agentManager = agentManager;
	}
	
	protected DeploymentVersionService deploymentVersionService = null;
	public DeploymentVersionService getDeploymentVersionService()
	{
		return deploymentVersionService;
	}
	public void setDeploymentVersionService(DeploymentVersionService deploymentVersionService)
	{
		this.deploymentVersionService = deploymentVersionService;
	}


	
	/**
	 * @param planResultKey
	 * @param config
	 * @param contextAdjustment
	 * @throws NumberFormatException
	 */
	public void performLabel(PlanResultKey planResultKey, Map<String,String> config, int contextAdjustment, BuildState status)
		throws NumberFormatException
	{
		final boolean labelSuccessOnly = "true".equalsIgnoreCase((StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSUCCESSONLY))));
		if( labelSuccessOnly && status.equals(BuildState.FAILED) )
		{
			log.debug("Build failed...skipping label operations.");
			return;
		}

		final String groupingLabel = StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_GROUPINGLABEL));
		final String groupingLabelDelete = StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_GROUPINGLABELDELETE));
		final String expireLabel = StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_EXPIRELABEL));
		final String labelsToRetainString = StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTORETAIN));
		final String labelsToIgnoreString = StringUtils.trim(config.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTOIGNORE));

		final List<String> labelsToIgnore = new ArrayList<String>();
		
		if( labelsToIgnoreString != null && labelsToIgnoreString.length() > 0 )
		{
			for( String l : labelsToIgnoreString.split(",") )
			{
				log.trace("Adding {} to label ignore list.", l);
				l = l.trim().toLowerCase();
				labelsToIgnore.add(l);
			}
		}
		
		final int labelsToRetain;
		labelsToRetain = Integer.parseInt(labelsToRetainString);

		log.debug("Performing Label Operation.  planResultKey={} groupingLabel={} groupingLabelDelete={} expireLabel={} labelsToRetain={} labelsToIgnore={}", planResultKey, groupingLabel, groupingLabelDelete, expireLabel, labelsToRetainString, labelsToIgnoreString);
		addLabels(planResultKey, groupingLabel, expireLabel);
		expireLabels(planResultKey.getPlanKey().getKey(), groupingLabel, expireLabel,labelsToRetain - contextAdjustment, "true".equalsIgnoreCase(groupingLabelDelete), labelsToIgnore);
	}

	/**
	 * This function performs the assignment of the groupingLabel and the expireLabel to
	 * the current build. 
	 * 
	 * @param planResultKey The Plan Result Key for the build to assign the labels to.
	 * @param groupingLabel The grouping label.
	 * @param expireLabel The expire label.
	 */
	public void addLabels(PlanResultKey planResultKey, String groupingLabel, String expireLabel)
	{
		log.debug("Adding groupingLabel label {} to {}", groupingLabel, planResultKey);
		labelManager.addLabel(groupingLabel, planResultKey, null);
		if( ! groupingLabel.equalsIgnoreCase(expireLabel) )
		{
			log.debug("Adding expireLabel {} to {}", expireLabel, planResultKey);
			labelManager.addLabel(expireLabel, planResultKey, null);
		}
	}
	
	public void expireLabels(final String planKey, final String groupingLabel, final String expireLabel, final int labelsToRetain, final boolean groupingLabelDelete, final List<String> labelsToIgnore)
	{
		transactionTemplate.execute(new TransactionCallback<Object>()
		{
			/* (non-Javadoc)
			 * @see com.atlassian.sal.api.transaction.TransactionCallback#doInTransaction()
			 */
			@Override
			public Object doInTransaction()
			{
				ResultsSummaryCriteria rsc = new ResultsSummaryCriteria(planKey);
				rsc.setMatchesLabels(new ArrayList<Label>(labelManager.getLabelsByName(Arrays.asList(new String[]{groupingLabel}))));
				
				int i = 0;
				for( ResultsSummary rs : resultsSummaryManager.getResultSummaries(rsc) )
				{
					log.trace("Iterating through result summaries to delete labels.");
					log.trace("Item#{}, PlanResultKey={}",i, rs.getPlanResultKey());
					i++;
					if( i <= labelsToRetain )
					{
						continue;
					}
					final PlanResultKey prk = rs.getPlanResultKey();
					if( groupingLabelDelete )
					{
						log.debug("Removing groupingLabel {} from {}.", groupingLabel, prk);
						labelManager.removeLabel(groupingLabel, prk, null);
					}
					if( CollectionUtils.containsAny(labelsToIgnore, rs.getLabelNames() ) )
					{
						log.debug("Found ignoreLabel on {}.  Skipping expireLabel removal.", prk );
						i--;
					}
					else
					{
						log.debug("Removing expireLabel {} from {}.", expireLabel, prk);
						labelManager.removeLabel(expireLabel, prk, null);
					}
				}
				return null;
			}
		});
	}
}
