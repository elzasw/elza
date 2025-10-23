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

	public ApIssue(ExistingIssueXml issue) {
		uuid = issue.getUuid().getValue();
		severity = issue.getSeverity().toString();
		message = issue.getMessage().getValue();
		ruleCode = issue.getRuleCode() != null? issue.getRuleCode().getValue() : null;
		issueCode = issue.getIssueCode() != null ? issue.getIssueCode().getValue() : null;
		source = issue.getSource() != null ? issue.getSource().getValue() : null;
		detail = issue.getDetail() != null ? issue.getDetail().getValue() : null;
		from = issue.getFrom().getValue();
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

	@Override
	public String toString() {
		return "ApIssue [severity=" + severity + ", message=" + message + "]";
	}

	public static List<ApIssue> createList(BatchChangeFailureXml failure) {
		List<ApIssue> issueList = new ArrayList<>();
		for (EntityIssuesXml entityIssue : failure.getIssues()) {
			// list of ExistingIssueXml.class -> list of IssueResult.class
			for (ExistingIssueXml issue : entityIssue.getIssue()) {
				issueList.add(new ApIssue(issue));
			}
		}
		return issueList;
	}
}
