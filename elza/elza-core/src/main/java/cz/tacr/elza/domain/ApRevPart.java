package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ap_rev_part")
public class ApRevPart implements AccessPointPart {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer partId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "part_type_id", nullable = false)
    private RulPartType partType;

    @Column(name = "part_type_id", updatable = false, insertable = false)
    private Integer partTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "parent_part_id")
    private ApPart parentPart;

    @Column(name = "parent_part_id", updatable = false, insertable = false)
    private Integer parentPartId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevPart.class)
    @JoinColumn(name = "rev_parent_part_id")
    private ApRevPart revParentPart;

    @Column(name = "rev_parent_part_id", updatable = false, insertable = false)
    private Integer revParentPartId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private ApChange createChange;

    @Column(name = "create_change_id", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "delete_change_id")
    private ApChange deleteChange;

    @Column(name = "delete_change_id", updatable = false, insertable = false)
    private Integer deleteChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevision.class)
    @JoinColumn(name = "revision_id", nullable = false)
    private ApRevision revision;

    @Column(name = "revision_id", nullable = false, updatable = false, insertable = false)
    private Integer revisionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "orig_part_id")
    private ApPart originalPart;

    @Column(name = "orig_part_id", updatable = false, insertable = false)
    private Integer originalPartId;

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(final Integer partId) {
        this.partId = partId;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public void setPartType(final RulPartType partType) {
        this.partType = partType;
        this.partTypeId = partType != null ? partType.getPartTypeId() : null;
    }

    public ApPart getParentPart() {
        return parentPart;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParentPart(ApPart parentPart) {
        this.parentPart = parentPart;
        this.parentPartId = parentPart != null ? parentPart.getPartId() : null;
    }

    public ApRevPart getRevParentPart() {
        return revParentPart;
    }

    public Integer getRevParentPartId() {
        return revParentPartId;
    }

    public void setRevParentPart(ApRevPart revParentPart) {
        this.revParentPart = revParentPart;
        this.revParentPartId = revParentPart != null ? revParentPart.getPartId() : null;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public ApRevision getRevision() {
        return revision;
    }

    public void setRevision(ApRevision revision) {
        this.revision = revision;
        this.revisionId = revision != null ? revision.getRevisionId() : null;
    }

    public Integer getRevisionId() {
        if (revisionId != null) {
            return revisionId;
        } else if (revision != null) {
            return revision.getRevisionId();
        } else {
            return null;
        }
    }

    public Integer getPartTypeId() {
        return partTypeId;
    }

    public ApPart getOriginalPart() {
        return originalPart;
    }

    public void setOriginalPart(ApPart originalPart) {
        this.originalPart = originalPart;
        this.originalPartId = originalPart != null ? originalPart.getPartId() : null;
    }

    public Integer getOriginalPartId() {
        return originalPartId;
    }
}
