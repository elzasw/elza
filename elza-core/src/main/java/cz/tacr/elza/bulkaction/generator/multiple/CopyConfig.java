package cz.tacr.elza.bulkaction.generator.multiple;

/**
 * Copy action configuration
 */
public class CopyConfig implements ActionConfig {

	protected String inputType;

	protected String outputType;

	/**
	 * Provést distinct při vracení výsledků?
	 */
	private boolean distinct;

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
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
