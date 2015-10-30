[@ww.textfield name='groupingLabel' label='Grouping Label' required='true' description='The label to use for grouping.' /]

[@ww.checkbox name='groupingLabelDelete' label='Delete Grouping Label' required='true' description='Check to delete the grouping label upon expiry.' /]

[@ww.textfield name='expireLabel' label='Expire Label' required=true description='The label to create and remove based on expiry.' /]

[@ww.textfield name='labelsToRetain' label='Number of Labeled Builds to Retain' required=true description='The number of builds labeled with the grouping label to keep before expiring the oldest' /]

[@ww.textfield name='labelsToIgnore' label='Labels to Ignore' required=true description='Comma separated list of labels to ignore.' /]
