package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Verze pojmenovaného výstupu z archivního výstupu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutput {

    public static final String TABLE_NAME = "arr_output";

    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_LOCK_CHANGE_ID = "lockChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer outputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_LOCK_CHANGE_ID)
    private ArrChange lockChange;

    /**
     * @return  identifikátor entity
     */
    public Integer getOutputId() {
        return outputId;
    }

    /**
     * @param outputId  identifikátor entity
     */
    public void setOutputId(final Integer outputId) {
        this.outputId = outputId;
    }

    /**
     * @return výstup z archivního souboru
     */
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    /**
     * @param outputDefinition výstup z archivního souboru
     */
    public void getOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    /**
     * @return změna vytvoření
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange změna vytvoření
     */
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return změna uzamčení
     */
    public ArrChange getLockChange() {
        return lockChange;
    }

    /**
     * @param lockChange změna uzamčení
     */
    public void setLockChange(final ArrChange lockChange) {
        this.lockChange = lockChange;
    }
}
