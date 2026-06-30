package cz.tacr.elza.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Seskupení operací zasahujících více archivních souborů najednou.
 *
 * Na tento záznam se odkazují související tabulky (např. arr_bulk_action_run),
 * čímž se jednotlivé per-fond záznamy spojí do jedné vícefondové operace.
 */
@Entity(name = "arr_funds_change")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFundsChange {

    public static final String TABLE_NAME = "arr_funds_change";

    @Id
    @GeneratedValue
    @Column(name = "funds_change_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer fundsChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = StringLength.LENGTH_50, nullable = false)
    private FundsChangeType changeType;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "create_date", nullable = false)
    private Date createDate;

    public ArrFundsChange() {
    }

    public static ArrFundsChange create(final FundsChangeType changeType,
                                        final Integer userId,
                                        final Date createDate) {
        ArrFundsChange fundsChange = new ArrFundsChange();
        fundsChange.changeType = changeType;
        fundsChange.userId = userId;
        fundsChange.createDate = createDate;
        return fundsChange;
    }

    public Integer getFundsChangeId() {
        return fundsChangeId;
    }

    public void setFundsChangeId(final Integer fundsChangeId) {
        this.fundsChangeId = fundsChangeId;
    }

    public FundsChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(final FundsChangeType changeType) {
        this.changeType = changeType;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }
}
