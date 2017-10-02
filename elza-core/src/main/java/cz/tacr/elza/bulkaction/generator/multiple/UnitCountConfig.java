package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class UnitCountConfig implements ActionConfig {

	protected String outputType;

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	/**
	 * Name of column where to store Unit type
	 */
	private String outputColumnUnitName;

	/**
	 * Name of column where to store Unit count
	 */
	private String outputColumnUnitCount;

	private List<UnitCounterConfig> aggegators;

	public List<UnitCounterConfig> getAggegators() {
		return aggegators;
	}

	public void setAggegators(List<UnitCounterConfig> aggegators) {
		this.aggegators = aggegators;
	}

	public String getOutputColumnUnitName() {
		return outputColumnUnitName;
	}

	public void setOutputColumnUnitName(String outputColumnUnitName) {
		this.outputColumnUnitName = outputColumnUnitName;
	}

	public String getOutputColumnUnitCount() {
		return outputColumnUnitCount;
	}

	public void setOutputColumnUnitCount(String outputColumnUnitCount) {
		this.outputColumnUnitCount = outputColumnUnitCount;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return UnitCountAction.class;
	}

}
