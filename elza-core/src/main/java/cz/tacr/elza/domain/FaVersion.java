package cz.tacr.elza.domain;

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
@Entity(name = "arr_fa_version")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FaVersion extends EntityBase {

    @Id
    @GeneratedValue
    private Integer faVersionId;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private FaChange createChange;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer lockChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "lockChangeId", nullable = true)
    private FaChange lockChange;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer rootFaLevelId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaLevel.class)
    @JoinColumn(name = "rootFaLevelId", nullable = true)
    private FaLevel rootFaLevel;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer findingAidId;
 
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private FindingAid findingAid;

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

    public Integer getFaVersionId() {
        return faVersionId;
    }

    public void setFaVersionId(Integer faVersionId) {
        this.faVersionId = faVersionId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public FaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(FaChange createChange) {
        this.createChange = createChange;
        if (createChange != null) {
            this.createChangeId = createChange.getChangeId();
        }
    }

    public Integer getLockChangeId() {
        return lockChangeId;
    }

    public void setLockChangeId(Integer lockChangeId) {
        this.lockChangeId = lockChangeId;
    }

    public FaChange getLockChange() {
        return lockChange;
    }

    public void setLockChange(FaChange lockChange) {
        this.lockChange = lockChange;
        if (lockChange != null) {
            this.lockChangeId = lockChange.getChangeId();
        }
    }

    public Integer getRootFaLevelId() {
        return rootFaLevelId;
    }

    public void setRootFaLevelId(Integer rootFaLevelId) {
        this.rootFaLevelId = rootFaLevelId;
    }

    public FaLevel getRootFaLevel() {
        return rootFaLevel;
    }

    public void setRootFaLevel(FaLevel rootFaLevel) {
        this.rootFaLevel = rootFaLevel;
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

    @Override
    public String toString() {
        return "FaVersion pk=" + faVersionId;
    }
}
