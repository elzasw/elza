package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Skupina souborů k DAO.
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_file_group")
public class ArrDaoFileGroup {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoFileGroupId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(name = "daoId", nullable = false, insertable = false, updatable = false)
    private Integer daoId;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    @Column(length = StringLength.LENGTH_1000, unique = true)
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
        this.daoId = dao == null ? null : dao.getDaoId();
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

    public Integer getDaoId() {
        return daoId;
    }
}
