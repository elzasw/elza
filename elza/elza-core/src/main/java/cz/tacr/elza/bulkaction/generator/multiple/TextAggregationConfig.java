package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class TextAggregationConfig implements ActionConfig {

	protected List<String> inputTypes;

	protected String outputType;

	/**
	 * Ignorovat duplikáty?
	 */
    protected boolean ignoreDuplicated = true;

	/**
	 * Flag if text item should be created for empty result
	 */
	protected boolean createEmpty;

    /**
     * Flag to aggregate data only from root nodes
     * 
     * This flag will aggregate only connected nodes and their predecessors.
     */
    protected boolean onlyRoots = false;

    public boolean isOnlyRoots() {
        return onlyRoots;
    }

    public void setOnlyRoots(boolean onlyRoots) {
        this.onlyRoots = onlyRoots;
    }

    public boolean isCreateEmpty() {
		return createEmpty;
	}

	public void setCreateEmpty(boolean createEmpty) {
		this.createEmpty = createEmpty;
	}

	public boolean isIgnoreDuplicated() {
		return ignoreDuplicated;
	}

	public void setIgnoreDuplicated(boolean ignoreDuplicated) {
		this.ignoreDuplicated = ignoreDuplicated;
	}

	public List<String> getInputTypes() {
		return inputTypes;
	}

	public void setInputTypes(List<String> inputTypes) {
		this.inputTypes = inputTypes;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return TextAggregationAction.class;
	}

}
