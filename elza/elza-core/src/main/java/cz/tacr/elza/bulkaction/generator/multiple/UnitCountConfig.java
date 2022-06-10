package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class UnitCountConfig implements ActionConfig {

	protected String outputType;

	/**
	 * Name of column where to store Unit type
	 */
	private String outputColumnUnitName;

	/**
	 * Name of column where to store Unit count
	 */
	private String outputColumnUnitCount;

	private String outputColumnDateRange;

    private DateRangeConfig dateRangeCounter;

	private List<UnitCounterConfig> aggegators;

    private boolean local;

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

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

	public String getOutputColumnDateRange() {
        return outputColumnDateRange;
    }

    public void setOutputColumnDateRange(String outputColumnDateRange) {
        this.outputColumnDateRange = outputColumnDateRange;
    }

    public DateRangeConfig getDateRangeCounter() {
        return dateRangeCounter;
    }

    public void setDateRangeCounter(DateRangeConfig dateRangeCounter) {
        this.dateRangeCounter = dateRangeCounter;
    }

    public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	@Override
	public Class<? extends Action> getActionClass() {
		return UnitCountAction.class;
	}

}
