package com.trusolve.atlassian.bamboo.plugins.tagexpirationtask.task;
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


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.core.util.PairType;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class TagExpirationTaskConfigurator extends AbstractTaskConfigurator
{
	public static final String TAGEXPIRATION_RECORDTAG = "groupingTag";
	public static final String TAGEXPIRATION_EXPIRETAG = "expireTag";
	public static final String TAGEXPIRATION_TAGSTORETAIN = "tagsToRetain";

    private static final Set<String> FIELDS = ImmutableSet.of(
    		TAGEXPIRATION_RECORDTAG,
    		TAGEXPIRATION_EXPIRETAG,
    		TAGEXPIRATION_TAGSTORETAIN
    );
    
    
    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context)
    {
        super.populateContextForCreate(context);
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS);
    }
    
    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS);
        return config;
    }
    
    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        if (StringUtils.isEmpty(params.getString(TagExpirationTaskConfigurator.TAGEXPIRATION_RECORDTAG)))
        {
            errorCollection.addError(TagExpirationTaskConfigurator.TAGEXPIRATION_RECORDTAG, "Please specify the recording tag.");
        }
        if (StringUtils.isEmpty(params.getString(TagExpirationTaskConfigurator.TAGEXPIRATION_EXPIRETAG)))
        {
            errorCollection.addError(TagExpirationTaskConfigurator.TAGEXPIRATION_EXPIRETAG, "Please specify the expiry tag (tag to add/remove based on policy).");
        }
        if (StringUtils.isEmpty(params.getString(TagExpirationTaskConfigurator.TAGEXPIRATION_TAGSTORETAIN)))
        {
            errorCollection.addError(TagExpirationTaskConfigurator.TAGEXPIRATION_TAGSTORETAIN, "Please specify the number of tags to retain.");
        }
        try
        {
        	Integer.parseInt(params.getString(TagExpirationTaskConfigurator.TAGEXPIRATION_TAGSTORETAIN));
        }
        catch( Exception e )
        {
        	errorCollection.addError(TagExpirationTaskConfigurator.TAGEXPIRATION_TAGSTORETAIN, "Please specify a valid integer for the number of tags to retain.");
        }
    }
}
