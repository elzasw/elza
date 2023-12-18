package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.Length;
import org.hibernate.annotations.Type;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Digitální archivní objekt (digitalizát).
 *
 */
@Table
@Entity(name = "arr_dao")
public class ArrDao {

    public final static String FIELD_DAO_ID = "daoId";

    static public enum DaoType {
        /**
         * Dao připojované k JP
         */
        ATTACHMENT,

        /**
         * Dao vytvářející JP
         */
        LEVEL
    };

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

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = false)
    private DaoType daoType;

    @Column(length = Length.LONG, nullable = false) // Hibernate long text field
    private String attributes;

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

    public DaoType getDaoType() {
        return daoType;
    }

    public void setDaoType(DaoType daoType) {
        this.daoType = daoType;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
}
