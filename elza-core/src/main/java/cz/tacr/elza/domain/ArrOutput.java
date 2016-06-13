package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutput}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutput implements cz.tacr.elza.api.ArrOutput<ArrOutputDefinition, ArrChange> {

    @Id
    @GeneratedValue
    private Integer outputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lockChangeId")
    private ArrChange lockChange;

    @Override
    public Integer getOutputId() {
        return outputId;
    }

    @Override
    public void setOutputId(final Integer outputId) {
        this.outputId = outputId;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    @Override
    public void getOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
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
}
