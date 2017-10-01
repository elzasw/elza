package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

/**
 * Copy action configuration
 */
public class CopyConfig implements ActionConfig {

	protected List<String> inputTypes;

	protected String outputType;

	/**
	 * Provést distinct při vracení výsledků?
	 */
	private boolean distinct;

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

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return CopyAction.class;
	}

}
