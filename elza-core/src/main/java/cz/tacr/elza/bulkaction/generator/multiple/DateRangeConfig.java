package cz.tacr.elza.bulkaction.generator.multiple;

public class DateRangeConfig implements ActionConfig {

	String inputType;

	String inputTypePrior;
	String inputTypePosterior;
	String outputType;

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

	public String getInputTypePrior() {
		return inputTypePrior;
	}

	public void setInputTypePrior(String inputTypePrior) {
		this.inputTypePrior = inputTypePrior;
	}

	public String getInputTypePosterior() {
		return inputTypePosterior;
	}

	public void setInputTypePosterior(String inputTypePosterior) {
		this.inputTypePosterior = inputTypePosterior;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return DateRangeAction.class;
	}
}
