package com.trusolve.atlassian.bamboo.plugins.labelexpiration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.amazonaws.util.StringUtils;
import com.atlassian.bamboo.buildqueue.manager.AgentManager;
import com.atlassian.bamboo.deployments.results.service.DeploymentResultService;
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


	public void performLabel(PlanResultKey planResultKey, Map<String,String> config, int contextAdjustment)
		throws Exception
	{
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
				l = l.trim().toLowerCase();
				labelsToIgnore.add(l);
			}
		}
		
		final int labelsToRetain;
		try
		{
			labelsToRetain = Integer.parseInt(labelsToRetainString);
		}
		catch (Exception e)
		{
			throw new Exception("Problem parsing label retention", e);
		}

		log.debug("Performing Label Operation.  planResultKey=%s groupingLabel=%s groupingLabelDelete=%s expireLabel=%s labelsToRetain=%s labelsToIgnore=%s", planResultKey.toString(), groupingLabel, groupingLabelDelete, expireLabel, labelsToRetainString, labelsToIgnoreString);
		addLabels(planResultKey, groupingLabel, expireLabel);
		expireLabels(planResultKey.getPlanKey().getKey(), groupingLabel, expireLabel,labelsToRetain - contextAdjustment, "true".equalsIgnoreCase(groupingLabelDelete), labelsToIgnore);
	}

	public void addLabels(PlanResultKey planResultKey, String groupingLabel, String expireLabel)
	{
		log.debug("Adding groupingLabel label %s to %s", groupingLabel, planResultKey.toString());
		labelManager.addLabel(groupingLabel, planResultKey, null);
		if( ! groupingLabel.equalsIgnoreCase(expireLabel) )
		{
			log.debug("Adding expireLabel %s to %s", expireLabel, planResultKey.toString());
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
					i++;
					if( i <= labelsToRetain )
					{
						continue;
					}
					final PlanResultKey prk = rs.getPlanResultKey();
					if( groupingLabelDelete )
					{
						labelManager.removeLabel(groupingLabel, prk, null);
					}
					if( CollectionUtils.containsAny(labelsToIgnore, rs.getLabelNames() ) )
					{
						i--;
					}
					else
					{
						labelManager.removeLabel(expireLabel, prk, null);
					}
				}
				return null;
			}
		});
	}
}
