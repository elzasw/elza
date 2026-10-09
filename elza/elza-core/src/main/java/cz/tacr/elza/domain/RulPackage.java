package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Length;
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

    /**
     * Version of a package marked for the import at the next start: the row holds the description
     * of the package from its file and nothing else; the startup import treats it as installed and
     * imports the file (see {@code AutoImportSelection}).
     */
    public static final int PENDING_VERSION = 0;

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer packageId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = Length.LONG, nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer version;

    /** Source language of the texts the package defines. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysLanguage.class)
    @JoinColumn(name = "languageId", nullable = false)
    private SysLanguage language;

    @Column(insertable = false, updatable = false)
    private Integer languageId;


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
    /** Marked for the import at the next start, with no content imported yet. */
    public boolean isPending() {
        return version != null && version == PENDING_VERSION;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public SysLanguage getLanguage() {
        return language;
    }

    public void setLanguage(final SysLanguage language) {
        this.language = language;
        this.languageId = language != null ? language.getLanguageId() : null;
    }

    public Integer getLanguageId() {
        return languageId;
    }
}
