package com.trusolve.atlassian.bamboo.plugins.labelexpiration.tasks;
/* Copyright 2015 TruSolve, LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import com.amazonaws.util.StringUtils;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.labels.Label;
import com.atlassian.bamboo.labels.LabelManager;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryCriteria;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.agent.AgentStatus;
import com.atlassian.bamboo.v2.build.agent.capability.AgentContext;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.trusolve.atlassian.bamboo.plugins.labelexpiration.LabelExpirationCore;

public class LabelExpirationTask
	extends LabelExpirationCore
	implements CommonTaskType
{

	@Override
	public TaskResult execute(CommonTaskContext taskContext) throws TaskException
	{
		final TaskResultBuilder builder = TaskResultBuilder.newBuilder(taskContext);
		final BuildLogger buildLogger = taskContext.getBuildLogger();

		if( resultsSummaryManager == null || labelManager == null || planManager == null || transactionTemplate == null )
		{
			// this must be running on a remote agent.
			buildLogger.addBuildLogEntry("Deferring labeling operations since this appears to be running on a remote agent.");
			return builder.build();
		}
		
		final ConfigurationMap config = taskContext.getConfigurationMap();


		try
		{
			PlanResultKey planResultKey = null;
			int adjustedLabelsToRetain = 0;
			
			if( taskContext instanceof TaskContext )
			{
				TaskContext buildContext = (TaskContext) taskContext;
				planResultKey = buildContext.getBuildContext().getParentBuildContext().getPlanResultKey();
				// decrement the labelsToRetain since the currently building result will get the label, but won't be present in the build label search.
				adjustedLabelsToRetain = 1;
			}
			else if ( taskContext instanceof DeploymentTaskContext )
			{
				DeploymentTaskContext dtc = (DeploymentTaskContext) taskContext;
				planResultKey = PlanKeys.getPlanResultKey(dtc.getDeploymentContext().getVariableContext().getEffectiveVariables().get("buildResultKey").getValue());
			}
			else
			{
				buildLogger.addErrorLogEntry("Task does not appear to be of type build or deployment");
				builder.failed();
				return builder.build();
			}
			this.performLabel(planResultKey, config, adjustedLabelsToRetain);
		}
		catch (Exception e)
		{
			buildLogger.addErrorLogEntry("Exception: " + e.getMessage());
			builder.failed();
		}
		return builder.build();
	}
}
