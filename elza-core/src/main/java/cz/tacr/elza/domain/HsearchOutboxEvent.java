package cz.tacr.elza.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity(name = "hsearch_outbox_event")
public class HsearchOutboxEvent {

	@Id
	@Column(nullable = false)
	private UUID id;

	private Integer entityIdHash;

	private Integer retries;

	@Column(nullable = false)
	private OffsetDateTime processAfter;

	private String entityId;

	private String entityName;

	private String status;

	private String tenantId;

	@Lob
	private byte[] payload;

	public HsearchOutboxEvent() {

	}

	public Integer getEntityIdHash() {
		return entityIdHash;
	}

	public void setEntityIdHash(Integer entityIdHash) {
		this.entityIdHash = entityIdHash;
	}

	public Integer getRetries() {
		return retries;
	}

	public void setRetries(Integer retries) {
		this.retries = retries;
	}

	public OffsetDateTime getProcessAfter() {
		return processAfter;
	}

	public void setProcessAfter(OffsetDateTime processAfter) {
		this.processAfter = processAfter;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
}
