package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import org.hibernate.Length;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * CAM-side issue/finding attached to an entity binding. Issues are not
 * versioned in Elza — only the current state synced from CAM is kept and
 * rewritten on every sync.
 *
 * Issue references never cross external systems: {@code relatedBinding} (if
 * set) always points to a binding within the same external system as
 * {@code binding}.
 */
@Entity(name = "ap_binding_issue")
public class ApBindingIssue {

    public enum Severity {
        WARNING,
        ERROR,
    }

    public enum Status {
        IR_ALL_REVISIONS_OK,
        IR_FIX_NEEDED,
        IR_REVISION_OK,
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingIssueId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId", nullable = false)
    private ApBinding binding;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer bindingId;

    @Column(length = StringLength.LENGTH_36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private Status status;

    @Column(length = StringLength.LENGTH_50)
    private String ruleCode;

    @Column(length = StringLength.LENGTH_50)
    private String issueCode;

    @Column(length = Length.LONG)
    private String message;

    @Column(length = Length.LONG)
    private String source;

    @Column(length = Length.LONG)
    private String detail;

    @Column(length = Length.LONG)
    private String note;

    private OffsetDateTime issueFrom;

    @Column(length = StringLength.LENGTH_50)
    private String extFromRev;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "partId")
    private ApPart part;

    @Column(updatable = false, insertable = false)
    private Integer partId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApItem.class)
    @JoinColumn(name = "itemId")
    private ApItem item;

    @Column(updatable = false, insertable = false)
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "relatedBindingId")
    private ApBinding relatedBinding;

    @Column(updatable = false, insertable = false)
    private Integer relatedBindingId;

    public Integer getBindingIssueId() {
        return bindingIssueId;
    }

    public void setBindingIssueId(Integer bindingIssueId) {
        this.bindingIssueId = bindingIssueId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
    }

    public Integer getBindingId() {
        return bindingId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getIssueFrom() {
        return issueFrom;
    }

    public void setIssueFrom(OffsetDateTime issueFrom) {
        this.issueFrom = issueFrom;
    }

    public String getExtFromRev() {
        return extFromRev;
    }

    public void setExtFromRev(String extFromRev) {
        this.extFromRev = extFromRev;
    }

    public ApPart getPart() {
        return part;
    }

    public void setPart(ApPart part) {
        this.part = part;
    }

    public Integer getPartId() {
        return partId;
    }

    public ApItem getItem() {
        return item;
    }

    public void setItem(ApItem item) {
        this.item = item;
    }

    public Integer getItemId() {
        return itemId;
    }

    public ApBinding getRelatedBinding() {
        return relatedBinding;
    }

    public void setRelatedBinding(ApBinding relatedBinding) {
        this.relatedBinding = relatedBinding;
    }

    public Integer getRelatedBindingId() {
        return relatedBindingId;
    }
}
