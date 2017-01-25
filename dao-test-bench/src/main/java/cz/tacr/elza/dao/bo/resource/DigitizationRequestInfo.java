package cz.tacr.elza.dao.bo.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import cz.tacr.elza.ws.types.v1.DigitizationRequest;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "digitizationRequestInfo")
public class DigitizationRequestInfo extends DigitizationRequest {

	public enum Status {
		PENDING, FINISHED, REVOKED;
	}

	@XmlAttribute
	private Status status = Status.PENDING;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
