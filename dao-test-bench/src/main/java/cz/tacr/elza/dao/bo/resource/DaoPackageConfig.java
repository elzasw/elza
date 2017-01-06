package cz.tacr.elza.dao.bo.resource;

public class DaoPackageConfig {

	private String identifier;

	private String fundIdentifier;

	private String batchIdentifier;

	private String batchLabel;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getFundIdentifier() {
		return fundIdentifier;
	}

	public void setFundIdentifier(String fundIdentifier) {
		this.fundIdentifier = fundIdentifier;
	}

	public String getBatchIdentifier() {
		return batchIdentifier;
	}

	public void setBatchIdentifier(String batchIdentifier) {
		this.batchIdentifier = batchIdentifier;
	}

	public String getBatchLabel() {
		return batchLabel;
	}

	public void setBatchLabel(String batchLabel) {
		this.batchLabel = batchLabel;
	}
}