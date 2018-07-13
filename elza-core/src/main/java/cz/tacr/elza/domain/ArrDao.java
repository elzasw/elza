package cz.tacr.elza.domain;

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
 * Digitální archivní objekt (digitalizát).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao")
public class ArrDao {

    @Id
    @GeneratedValue
    private Integer daoId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDaoPackage.class)
    @JoinColumn(name = "daoPackageId", nullable = false)
    private ArrDaoPackage daoPackage;

    @Column(name = "daoPackageId", nullable = false, insertable = false, updatable = false)
    private Integer daoPackageId;

    @Column(nullable = false)
    private Boolean valid;

    @Column(nullable = false, length = StringLength.LENGTH_1000, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250)
    private String label;

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(final Integer daoId) {
        this.daoId = daoId;
    }

    public ArrDaoPackage getDaoPackage() {
        return daoPackage;
    }

    public void setDaoPackage(final ArrDaoPackage daoPackage) {
        this.daoPackage = daoPackage;
        this.daoPackageId = daoPackage == null ? null : daoPackage.getDaoPackageId();
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(final Boolean valid) {
        this.valid = valid;
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

    public Integer getDaoPackageId() {
        return daoPackageId;
    }
}
