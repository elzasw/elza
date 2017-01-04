package cz.tacr.elza.dao.descriptor;

import java.util.ArrayList;
import java.util.List;

public class DaoRequestInfo implements Descriptor {

	private String identifier;

	private String requestIdentifier;

	private String systemIdentifier;

	private List<String> daoIdentifiers = new ArrayList<>();

	private String targetFund;

	private String description;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getRequestIdentifier() {
		return requestIdentifier;
	}

	public void setRequestIdentifier(String requestIdentifier) {
		this.requestIdentifier = requestIdentifier;
	}

	public String getSystemIdentifier() {
		return systemIdentifier;
	}

	public void setSystemIdentifier(String systemIdentifier) {
		this.systemIdentifier = systemIdentifier;
	}

	public List<String> getDaoIdentifiers() {
		return daoIdentifiers;
	}

	public void setDaoIdentifiers(List<String> daoIdentifiers) {
		this.daoIdentifiers = daoIdentifiers;
	}

	public String getTargetFund() {
		return targetFund;
	}

	public void setTargetFund(String targetFund) {
		this.targetFund = targetFund;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean isDirty() {
		return true;
	}
}
