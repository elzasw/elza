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
    private Integer rootNodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaLevel.class)
    @JoinColumn(name = "rootNodeId", nullable = true, referencedColumnName = "nodeId")
    private FaLevel rootNode;

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

    public void setFaVersionId(final Integer faVersionId) {
        this.faVersionId = faVersionId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public FaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final FaChange createChange) {
        this.createChange = createChange;
        if (createChange != null) {
            this.createChangeId = createChange.getChangeId();
        }
    }

    public Integer getLockChangeId() {
        return lockChangeId;
    }

    public void setLockChangeId(final Integer lockChangeId) {
        this.lockChangeId = lockChangeId;
    }

    public FaChange getLockChange() {
        return lockChange;
    }

    public void setLockChange(final FaChange lockChange) {
        this.lockChange = lockChange;
        if (lockChange != null) {
            this.lockChangeId = lockChange.getChangeId();
        }
    }

    public Integer getRootFaLevelId() {
        return rootNodeId;
    }

    public void setRootFaLevelId(final Integer rootFaLevelId) {
        this.rootNodeId = rootFaLevelId;
    }

    public FaLevel getRootNode() {
        return rootNode;
    }

    public void setRootNode(final FaLevel rootNode) {
        this.rootNode = rootNode;
        if (rootNode != null) {
            this.rootNodeId = rootNode.getNodeId();
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

    public Integer getRootNodeId() {
        return rootNodeId;
    }

    public void setRootNodeId(final Integer rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    public void setFindingAidId(final Integer findingAidId) {
        this.findingAidId = findingAidId;
    }

    public void setArrangementTypeId(final Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
