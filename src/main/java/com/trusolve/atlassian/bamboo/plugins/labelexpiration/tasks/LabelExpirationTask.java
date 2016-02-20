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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.VariableContext;
import com.trusolve.atlassian.bamboo.plugins.labelexpiration.LabelExpirationCore;

public class LabelExpirationTask
	extends LabelExpirationCore
	implements CommonTaskType
{
	private static final Logger log = LoggerFactory.getLogger(LabelExpirationTask.class);
	
	@Override
	public TaskResult execute(CommonTaskContext taskContext) throws TaskException
	{
		log.debug("Executing plugin Task.");
		final TaskResultBuilder builder = TaskResultBuilder.newBuilder(taskContext);
		final BuildLogger buildLogger = taskContext.getBuildLogger();
		
		// Variable substitution does not take place on Event execution so we need to get the 
		// task variables and persist them into the build context.  This should make them available
		// in the event.
		
		Map<String,String> customBuildData = taskContext.getCommonContext().getCurrentResult().getCustomBuildData();
		int pluginCount = 0;
		try
		{
			pluginCount = Integer.parseInt(customBuildData.get(LabelExpirationTaskConfigurator.LABELEXPIRATION_RESULTVARIABLEPREFIX + "pluginCount"));
			pluginCount++;
		}
		catch( Exception e )
		{
			// do nothing
		}
		String pluginCountString = Integer.toString(pluginCount);
		customBuildData.put(LabelExpirationTaskConfigurator.LABELEXPIRATION_RESULTVARIABLEPREFIX + "pluginCount", pluginCountString);
		for( Map.Entry<String,String> entry : taskContext.getConfigurationMap().entrySet() )
		{
			customBuildData.put(LabelExpirationTaskConfigurator.LABELEXPIRATION_RESULTVARIABLEPREFIX + pluginCountString + "." + entry.getKey(), entry.getValue());
		}

		buildLogger.addBuildLogEntry("Deferring labeling operations for post processing.");
		return builder.build();
	}
}
