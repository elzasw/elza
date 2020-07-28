package cz.tacr.elza.bulkaction.generator.multiple;

public class DateRangeConfig implements ActionConfig {

    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

	String inputType;

    String bulkRangeType;
	String outputType;

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

    public String getBulkRangeType() {
        return bulkRangeType;
	}

    public void setBulkRangeType(String bulkRangeType) {
        this.bulkRangeType = bulkRangeType;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

    @Override
	public Class<? extends Action> getActionClass() {
		return DateRangeAction.class;
	}
}
