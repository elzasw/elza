package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Soubor Fund
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "arr_file")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFile extends DmsFile {

    public static final String TABLE_NAME = "arr_file";

    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    public static final String FIELD_DELETE_CHANGE = "deleteChange";

    public static final String FIELD_FUND = "fund";

    @JsonIgnore
    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    protected ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, nullable = false, updatable = false, insertable = false)
    protected Integer createChangeId;

    @JsonIgnore
    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
    protected ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, nullable = true, updatable = false, insertable = false)
    protected Integer deleteChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    @JsonIgnore
    private ArrFund fund;

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ArrChange createChange) {
        this.createChange = createChange;
    }

    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
    }

}
