package cz.tacr.elza.dao.bo.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cz.tacr.elza.ws.types.v1.DigitizationRequest;

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
