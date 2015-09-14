package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Vytvářená nebo již schválená verze archivní pomůcky. Základem archivní pomůcky je hierarchický archivní
 * popis. Každá pomůcka je vytvářena podle určitých pravidel tvorby. Pravidla tvorby mohou definovat
 * různé typy finální pomůcky (například manipulační seznam, inventární seznam, katalog v případě
 * ZP)
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_version")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFaVersion extends AbstractVersionableEntity implements
        cz.tacr.elza.api.ArrFaVersion<ArrFindingAid, ArrFaChange, ArrFaLevel, RulArrangementType, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer faVersionId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "createFaChangeId", nullable = false)
    private ArrFaChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "lockFaChangeId", nullable = true)
    private ArrFaChange lockChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaLevel.class)
    @JoinColumn(name = "rootFaLevelId", nullable = false)
    private ArrFaLevel rootFaLevel;

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

    @Override
    public Integer getFaVersionId() {
        return faVersionId;
    }

    @Override
    public void setFaVersionId(final Integer faVersionId) {
        this.faVersionId = faVersionId;
    }

    @Override
    public ArrFaChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrFaChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrFaChange getLockChange() {
        return lockChange;
    }

    @Override
    public void setLockChange(final ArrFaChange lockChange) {
        this.lockChange = lockChange;
    }

    @Override
    public ArrFaLevel getRootFaLevel() {
        return rootFaLevel;
    }

    @Override
    public void setRootFaLevel(final ArrFaLevel rootFaLevel) {
        this.rootFaLevel = rootFaLevel;
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
    public String toString() {
        return "ArrFaVersion pk=" + faVersionId;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ArrFaVersion version = (ArrFaVersion) obj;

        if (faVersionId != null ? !faVersionId.equals(version.faVersionId) : version.faVersionId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return faVersionId != null ? faVersionId.hashCode() : 0;
    }
}
