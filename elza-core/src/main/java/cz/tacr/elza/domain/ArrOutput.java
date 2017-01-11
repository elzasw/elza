package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutput}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutput implements Serializable {

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

    public Integer getOutputId() {
        return outputId;
    }

    public void setOutputId(final Integer outputId) {
        this.outputId = outputId;
    }

    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void getOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    public ArrChange getLockChange() {
        return lockChange;
    }

    public void setLockChange(final ArrChange lockChange) {
        this.lockChange = lockChange;
    }
}
