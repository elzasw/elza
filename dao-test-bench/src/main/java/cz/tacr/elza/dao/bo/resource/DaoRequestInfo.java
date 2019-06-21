package cz.tacr.elza.dao.bo.resource;

import java.util.ArrayList;
import java.util.List;

public class DaoRequestInfo {

	public enum Status {
		PENDING, FINISHED, REVOKED;
	}

	private String requestIdentifier;

	private String systemIdentifier;

	private List<String> daoIdentifiers = new ArrayList<>();

	private String targetFund;

	private String description;

	private Status status;

	private String username;

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

	public Status getStatus() {
		return status != null ? status : Status.PENDING;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
