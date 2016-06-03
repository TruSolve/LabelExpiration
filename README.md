# LabelExpiration
Bamboo Label Expiration Plugin

The LabelExpiration plugins performs specialized labeling operations on build plans.  It can be added as a task to a build or deployment plan.  Build plans are labeled based on the configuration of the task.  Labels are removed during execution that meet configured criteria.

* Label Successful Builds Only - if this is checked, builds will only be labeled in the event of a successful build stage or deployment.
* Grouping Label - this label is applied to the current build that meet configured criteria.  It is used for grouping or counting when determining the number of labels to retain.
* Delete Grouping Label - if this is checked, the oldest "Grouping Label"s that exceed the count of "Number of Labeled Builds to Retain" are removed from those builds.  If this is not checked, this label is left intact indefinitely.
* Expire Label - this label is applied to the current build that meets configured criteria.  During plugin execution, this label is removed from the oldest builds with the grouping label present, retaining on the most recent.
* Number of Labeled Builds to Retain - this number of the most recent builds that are labeled with the grouping label are retained.
* Labels to Ignore - builds with this label are completely ignored by the plugin.

A typical use case for this plugin would be to perform more advanaced retention of builds based on their release state from deployment plans.  You might want to retain the last 2 Prod deployments, the last 5 QA deployments and the last 10 Dev deployments.  This can be achieved by doing the following:

1. Configure the Bamboo build expiration process to NOT expire builds with the tag "lock" on them.
2. Configuration the LabelExpiration plugin on your Dev deployment as follows:
  * Label Successful Builds Only - checked
  * Grouping Label - dev
  * Delete Grouping Label - unchecked
  * Expire Label - lock
  * Number of Labeled Builds to Retain - 10
  * Labels to Ignore - {NOTHING}
3. Configuration the LabelExpiration plugin on your QA deployment as follows:
  * Label Successful Builds Only - checked
  * Grouping Label - qa
  * Delete Grouping Label - unchecked
  * Expire Label - lock
  * Number of Labeled Builds to Retain - 5
  * Labels to Ignore - {NOTHING}
2. Configuration the LabelExpiration plugin on your Prod deployment as follows:
  * Label Successful Builds Only - checked
  * Grouping Label - prod
  * Delete Grouping Label - unchecked
  * Expire Label - lock
  * Number of Labeled Builds to Retain - 2
  * Labels to Ignore - {NOTHING}

