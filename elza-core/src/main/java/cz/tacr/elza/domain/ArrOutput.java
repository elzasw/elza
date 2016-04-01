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
public class ArrOutput implements cz.tacr.elza.api.ArrOutput<ArrNamedOutput, ArrChange> {

    @Id
    @GeneratedValue
    private Integer outputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNamedOutput.class)
    @JoinColumn(name = "namedOutputId", nullable = false)
    private ArrNamedOutput namedOutput;

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
    public ArrNamedOutput getNamedOutput() {
        return namedOutput;
    }

    @Override
    public void setNamedOutput(final ArrNamedOutput namedOutput) {
        this.namedOutput = namedOutput;
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
