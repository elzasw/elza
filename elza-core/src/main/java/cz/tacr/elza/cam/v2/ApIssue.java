package cz.tacr.elza.cam.v2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cz.tacr.cam.v2.schema.cam.BatchChangeFailureXml;
import cz.tacr.cam.v2.schema.cam.EntityIssuesXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;

public class ApIssue {

	private String uuid;

	private String severity;

	private String ruleCode;

	private String issueCode;

	private String message;

	private String source;

	private String detail;

	private LocalDateTime from;

	/** Resolved ELZA ApPart.partId for {@link ExistingIssueXml#getPartRef()}, or null. */
	private Integer partId;

	/** Resolved ELZA ApItem.itemId for {@link ExistingIssueXml#getItemRef()}, or null. */
	private Integer itemId;

	/** Resolved ELZA ApAccessPoint.accessPointId for {@link ExistingIssueXml#getEntityRef()}, or null. */
	private Integer entityId;

	/** Human-readable name of the referenced part (DISPLAY_NAME index, or part-type description as fallback). */
	private String partName;

	/** Human-readable name of the referenced item ({@code "typeDesc: value"} for scalar data, type description otherwise). */
	private String itemName;

	/** Human-readable name of the referenced other AP (DISPLAY_NAME of its preferred part). */
	private String entityName;

	public ApIssue() {
	}

	public ApIssue(ExistingIssueXml issue) {
		this(issue, null);
	}

	public ApIssue(ExistingIssueXml issue, IssueRefResolver resolver) {
		uuid = issue.getUuid().getValue();
		severity = issue.getSeverity().toString();
		message = issue.getMessage().getValue();
		ruleCode = issue.getRuleCode() != null? issue.getRuleCode().getValue() : null;
		issueCode = issue.getIssueCode() != null ? issue.getIssueCode().getValue() : null;
		source = issue.getSource() != null ? issue.getSource().getValue() : null;
		detail = issue.getDetail() != null ? issue.getDetail().getValue() : null;
		from = issue.getFrom().getValue();
		if (resolver != null) {
			partId = resolver.resolvePart(issue.getPartRef());
			itemId = resolver.resolveItem(issue.getItemRef());
			entityId = resolver.resolveEntity(issue.getEntityRef());
			// Ensure item-only refs also carry a partId so the frontend has a single
			// scroll target regardless of which ref CAM returned.
			if (partId == null && itemId != null) {
				partId = resolver.partIdForItem(itemId);
			}
			partName = resolver.resolvePartName(partId);
			itemName = resolver.resolveItemName(itemId);
			entityName = resolver.resolveEntityName(entityId);
		}
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getRuleCode() {
		return ruleCode;
	}

	public void setRuleCode(String ruleCode) {
		this.ruleCode = ruleCode;
	}

	public String getIssueCode() {
		return issueCode;
	}

	public void setIssueCode(String issueCode) {
		this.issueCode = issueCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public LocalDateTime getFrom() {
		return from;
	}

	public void setFrom(LocalDateTime from) {
		this.from = from;
	}

	public Integer getPartId() {
		return partId;
	}

	public void setPartId(Integer partId) {
		this.partId = partId;
	}

	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public String getPartName() {
		return partName;
	}

	public void setPartName(String partName) {
		this.partName = partName;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	@Override
	public String toString() {
		return "ApIssue [severity=" + severity + ", message=" + message + "]";
	}

	public static List<ApIssue> createList(BatchChangeFailureXml failure) {
		return createList(failure, null);
	}

	public static List<ApIssue> createList(BatchChangeFailureXml failure, IssueRefResolver resolver) {
		List<ApIssue> issueList = new ArrayList<>();
		for (EntityIssuesXml entityIssue : failure.getIssues()) {
			// list of ExistingIssueXml.class -> list of IssueResult.class
			for (ExistingIssueXml issue : entityIssue.getIssue()) {
				issueList.add(new ApIssue(issue, resolver));
			}
		}
		return issueList;
	}
}
