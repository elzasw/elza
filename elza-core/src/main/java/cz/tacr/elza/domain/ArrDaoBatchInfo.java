package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoBatchInfo}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_batch_info")
public class ArrDaoBatchInfo implements Serializable {

    @Id
    @GeneratedValue
    private Integer daoBatchInfoId;

    @Column(nullable = false, length = StringLength.LENGTH_50, unique = true)
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
