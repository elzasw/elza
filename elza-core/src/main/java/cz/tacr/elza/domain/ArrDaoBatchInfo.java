package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoBatchInfo}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_batch_info")
public class ArrDaoBatchInfo implements cz.tacr.elza.api.ArrDaoBatchInfo {

    @Id
    @GeneratedValue
    private Integer daoBatchInfoId;

    @Column(nullable = false, length = StringLength.LENGTH_50, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    @Override
    public Integer getDaoBatchInfoId() {
        return daoBatchInfoId;
    }

    @Override
    public void setDaoBatchInfoId(final Integer daoBatchInfoId) {
        this.daoBatchInfoId = daoBatchInfoId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(final String label) {
        this.label = label;
    }
}
