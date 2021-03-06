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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.ImmutableSet;

public class LabelExpirationTaskConfigurator extends AbstractTaskConfigurator
{
	private static final Logger log = LoggerFactory.getLogger(LabelExpirationTaskConfigurator.class);

	public static final String LABELEXPIRATION_RESULTVARIABLEPREFIX = "labelExpirationTask.";
	
	public static final String LABELEXPIRATION_GROUPINGLABEL = "groupingLabel";
	public static final String LABELEXPIRATION_GROUPINGLABELDELETE = "groupingLabelDelete";
	public static final String LABELEXPIRATION_LABELSUCCESSONLY = "labelSuccessOnly";
	public static final String LABELEXPIRATION_EXPIRELABEL = "expireLabel";
	public static final String LABELEXPIRATION_LABELSSTORETAIN = "labelsToRetain";
	public static final String LABELEXPIRATION_LABELSSTOIGNORE = "labelsToIgnore";

	private static final Set<String> FIELDS = ImmutableSet.of(
		LABELEXPIRATION_GROUPINGLABEL,
		LABELEXPIRATION_GROUPINGLABELDELETE,
		LABELEXPIRATION_LABELSUCCESSONLY,
		LABELEXPIRATION_EXPIRELABEL,
		LABELEXPIRATION_LABELSSTORETAIN,
		LABELEXPIRATION_LABELSSTOIGNORE);

	@Override
	public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
	{
		super.populateContextForView(context, taskDefinition);
		log.debug("Populating the view context.");
		taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS);
	}

	@Override
	public void populateContextForCreate(Map<String, Object> context)
	{
		// TODO Auto-generated method stub
		super.populateContextForCreate(context);
		context.put(LABELEXPIRATION_LABELSUCCESSONLY, "true");
	}

	@Override
	public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
	{
		super.populateContextForEdit(context, taskDefinition);
		log.debug("Populating the edit context.");
		taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS);
	}

	@NotNull
	@Override
	public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition)
	{
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
		log.debug("Generating the Task Config Map.");
		taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS);
		return config;
	}

	@Override
	public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection)
	{
		log.debug("Validating user input.");
		super.validate(params, errorCollection);

		if (StringUtils.isEmpty(params.getString(LabelExpirationTaskConfigurator.LABELEXPIRATION_GROUPINGLABEL)))
		{
			errorCollection.addError(LabelExpirationTaskConfigurator.LABELEXPIRATION_GROUPINGLABEL, "Please specify the recording label.");
		}
		if (StringUtils.isEmpty(params.getString(LabelExpirationTaskConfigurator.LABELEXPIRATION_EXPIRELABEL)))
		{
			errorCollection.addError(LabelExpirationTaskConfigurator.LABELEXPIRATION_EXPIRELABEL, "Please specify the expiry label (label to add/remove based on policy).");
		}
		if (StringUtils.isEmpty(params.getString(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTORETAIN)))
		{
			errorCollection.addError(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTORETAIN, "Please specify the number of labels to retain.");
		}
		try
		{
			Integer.parseInt(params.getString(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTORETAIN));
		}
		catch (Exception e)
		{
			errorCollection.addError(LabelExpirationTaskConfigurator.LABELEXPIRATION_LABELSSTORETAIN, "Please specify a valid integer for the number of labels to retain.");
		}
	}
}
