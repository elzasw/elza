package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.*;
import cz.tacr.elza.api.ArrNodeConformityInfo;


/**
 * Vytvářená nebo již schválená verze archivní pomůcky. Základem archivní pomůcky je hierarchický archivní
 * popis. Každá pomůcka je vytvářena podle určitých pravidel tvorby. Pravidla tvorby mohou definovat
 * různé typy finální pomůcky (například manipulační seznam, inventární seznam, katalog v případě
 * ZP)
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_finding_aid_version")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFindingAidVersion extends AbstractVersionableEntity implements
        cz.tacr.elza.api.ArrFindingAidVersion<ArrFindingAid, ArrChange, ArrLevel, RulArrangementType, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer findingAidVersionId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lockChangeId", nullable = true)
    private ArrChange lockChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrLevel.class)
    @JoinColumn(name = "rootLevelId", nullable = false)
    private ArrLevel rootLevel;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAid.class)
    @JoinColumn(name = "findingAidId", nullable = false)
    private ArrFindingAid findingAid;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulArrangementType.class)
    @JoinColumn(name = "arrangementTypeId", nullable = false)
    private RulArrangementType arrangementType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lastArrFaChangeId", nullable = false)
    private ArrChange lastChange;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = true)
    private ArrFindingAidVersion.State state;

    @Override
    public Integer getFindingAidVersionId() {
        return findingAidVersionId;
    }

    @Override
    public void setFindingAidVersionId(final Integer findingAidVersionId) {
        this.findingAidVersionId = findingAidVersionId;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getLockChange() {
        return lockChange;
    }

    @Override
    public void setLockChange(final ArrChange lockChange) {
        this.lockChange = lockChange;
    }

    @Override
    public ArrLevel getRootLevel() {
        return rootLevel;
    }

    @Override
    public void setRootLevel(final ArrLevel rootFaLevel) {
        this.rootLevel = rootFaLevel;
    }

    @Override
    public ArrFindingAid getFindingAid() {
        return findingAid;
    }

    @Override
    public void setFindingAid(final ArrFindingAid findingAid) {
        this.findingAid = findingAid;
    }

    @Override
    public RulArrangementType getArrangementType() {
        return arrangementType;
    }

    @Override
    public void setArrangementType(final RulArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public ArrChange getLastChange() {
        return lastChange;
    }

    @Override
    public void setLastChange(final ArrChange change) {
        this.lastChange = change;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "ArrFindingAidVersion pk=" + findingAidVersionId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        cz.tacr.elza.domain.ArrFindingAidVersion version = (cz.tacr.elza.domain.ArrFindingAidVersion) obj;

        if (findingAidVersionId != null ? !findingAidVersionId.equals(version.findingAidVersionId) : version.findingAidVersionId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return findingAidVersionId != null ? findingAidVersionId.hashCode() : 0;
    }
}
