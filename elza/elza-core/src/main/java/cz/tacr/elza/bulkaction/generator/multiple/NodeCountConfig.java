package cz.tacr.elza.bulkaction.generator.multiple;

public class NodeCountConfig implements ActionConfig {

    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

	protected String outputType;

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return NodeCountAction.class;
	}

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

}
