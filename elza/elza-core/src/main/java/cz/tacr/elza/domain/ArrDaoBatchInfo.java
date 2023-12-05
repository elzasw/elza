package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Přejímka.
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_batch_info")
public class ArrDaoBatchInfo {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoBatchInfoId;

    @Column(nullable = false, length = StringLength.LENGTH_1000, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    public Integer getDaoBatchInfoId() {
        return daoBatchInfoId;
    }

    public void setDaoBatchInfoId(final Integer daoBatchInfoId) {
        this.daoBatchInfoId = daoBatchInfoId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }
}
