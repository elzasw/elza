package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_version")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Version extends EntityBase {

    @Id
    @GeneratedValue
    private Integer versionId;

    @Column(nullable = false)
    private LocalDateTime createDate;

    @Column(nullable = true)
    private LocalDateTime approvalDate;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer arrangementTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrangementType.class)
    @JoinColumn(name = "arrangementTypeId", nullable = false)
    private ArrangementType arrangementType;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RuleSet ruleSet;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer findingAidId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private FindingAid findingAid;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(final Integer versionId) {
        this.versionId = versionId;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(final LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    public ArrangementType getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(final ArrangementType arrangementType) {
        this.arrangementType = arrangementType;
        if (arrangementType != null) {
            arrangementTypeId = arrangementType.getArrangementTypeId();
        }
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(final RuleSet ruleSet) {
        this.ruleSet = ruleSet;
        if (ruleSet != null) {
            ruleSetId = ruleSet.getRuleSetId();
        }
    }

    public Integer getFindingAidId() {
        return findingAidId;
    }

    public FindingAid getFindingAid() {
        return findingAid;
    }

    public void setFindingAid(final FindingAid findingAid) {
        this.findingAid = findingAid;
        if (findingAid != null) {
            findingAidId = findingAid.getFindigAidId();
        }
    }

    @Override
    public String toString() {
        return "Version pk=" + versionId;
    }
}
