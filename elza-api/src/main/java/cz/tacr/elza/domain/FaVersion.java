package cz.tacr.elza.domain;

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private FaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "lockChangeId", nullable = true)
    private FaChange lockChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaLevel.class)
    @JoinColumn(name = "rootNodeId", nullable = true, referencedColumnName = "nodeId")
    private FaLevel rootNode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private FindingAid findingAid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrangementType.class)
    @JoinColumn(name = "arrangementTypeId", nullable = false)
    private ArrangementType arrangementType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RuleSet ruleSet;

    public Integer getFaVersionId() {
        return faVersionId;
    }

    public void setFaVersionId(final Integer faVersionId) {
        this.faVersionId = faVersionId;
    }

    public FaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final FaChange createChange) {
        this.createChange = createChange;
    }

    public FaChange getLockChange() {
        return lockChange;
    }

    public void setLockChange(final FaChange lockChange) {
        this.lockChange = lockChange;
    }

    public FaLevel getRootNode() {
        return rootNode;
    }

    public void setRootNode(final FaLevel rootNode) {
        this.rootNode = rootNode;
    }

    public FindingAid getFindingAid() {
        return findingAid;
    }

    public void setFindingAid(final FindingAid findingAid) {
        this.findingAid = findingAid;
    }

    public ArrangementType getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(final ArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(final RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public String toString() {
        return "FaVersion pk=" + faVersionId;
    }
}
