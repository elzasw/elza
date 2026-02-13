package cz.tacr.elza.repository.vo;

public class AccessPointIdAssignToVO {

	public final Integer accessPointId;

	public final Integer assignTo;

	public AccessPointIdAssignToVO(Integer accessPointId, Integer assignTo) {
		this.accessPointId = accessPointId;
		this.assignTo = assignTo;
	}

	public Integer getAccessPointId() {
		return accessPointId;
	}

	public Integer getAssignTo() {
		return assignTo;
	}
}
