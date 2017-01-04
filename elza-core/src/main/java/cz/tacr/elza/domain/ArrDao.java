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
 * Implementace {@link cz.tacr.elza.api.ArrDao}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao")
public class ArrDao implements cz.tacr.elza.api.ArrDao<ArrDaoPackage> {

    @Id
    @GeneratedValue
    private Integer daoId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDaoPackage.class)
    @JoinColumn(name = "daoPackageId", nullable = false)
    private ArrDaoPackage daoPackage;

    @Column(nullable = false)
    private Boolean valid;

    @Column(nullable = false, length = StringLength.LENGTH_50, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    @Override
    public Integer getDaoId() {
        return daoId;
    }

    @Override
    public void setDaoId(final Integer daoId) {
        this.daoId = daoId;
    }

    @Override
    public ArrDaoPackage getDaoPackage() {
        return daoPackage;
    }

    @Override
    public void setDaoPackage(final ArrDaoPackage daoPackage) {
        this.daoPackage = daoPackage;
    }

    @Override
    public Boolean getValid() {
        return valid;
    }

    @Override
    public void setValid(final Boolean valid) {
        this.valid = valid;
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
