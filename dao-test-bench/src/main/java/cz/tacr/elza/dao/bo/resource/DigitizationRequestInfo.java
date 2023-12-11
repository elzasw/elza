package cz.tacr.elza.dao.bo.resource;

import cz.tacr.elza.ws.types.v1.DigitizationRequest;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = DigitizationRequestInfo.NAME)
public class DigitizationRequestInfo extends DigitizationRequest {

	public static final String NAME = "digitizationRequestInfo";
	
	public enum Status {
		PENDING, FINISHED, REVOKED;
	}
	
	@XmlAttribute
	private Status status;

	public Status getStatus() {
		return status != null ? status : Status.PENDING;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
