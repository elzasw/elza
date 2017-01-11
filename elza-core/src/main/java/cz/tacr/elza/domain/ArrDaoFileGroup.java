package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoFileGroup}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_file_group")
public class ArrDaoFileGroup implements Serializable {

    @Id
    @GeneratedValue
    private Integer daoFileGroupId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    @Column(length = StringLength.LENGTH_50, unique = true)
    private String code;

    public Integer getDaoFileGroupId() {
        return daoFileGroupId;
    }

    public void setDaoFileGroupId(final Integer daoFileGroupId) {
        this.daoFileGroupId = daoFileGroupId;
    }

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
