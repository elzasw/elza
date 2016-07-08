package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Implementace balíčku.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Entity(name = "rul_package")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulPackage implements cz.tacr.elza.api.RulPackage {

    @Id
    @GeneratedValue
    private Integer packageId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(nullable = false)
    private Integer version;


    @Override
    public Integer getPackageId() {
        return packageId;
    }

    @Override
    public void setPackageId(final Integer packageId) {
        this.packageId = packageId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(final Integer version) {
        this.version = version;
    }
}
