package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

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
public class RulPackage {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer packageId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(nullable = false)
    @Lob
    //@Type(type = "org.hibernate.type.TextType") TODO hibernate search 6
    private String description;

    @Column(nullable = false)
    private Integer version;


    /**
     * @return identifikátor entity
     */
    public Integer getPackageId() {
        return packageId;
    }

    /**
     * @param packageId identifikátor entity
     */
    public void setPackageId(final Integer packageId) {
        this.packageId = packageId;
    }

    /**
     * @return název balíčku
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název balíčku
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return kód balíčku
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code kód balíčku
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * @return popis
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description popis
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return verze balíčku
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version verze balíčku
     */
    public void setVersion(final Integer version) {
        this.version = version;
    }
}
