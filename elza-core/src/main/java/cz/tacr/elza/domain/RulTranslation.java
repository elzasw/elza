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

import org.hibernate.Length;

/**
 * One translated piece of package-provided text: field {@link #field} of the entity
 * {@link #entityType}/{@link #entityCode} in language {@link #language}, contributed by
 * {@link #rulPackage}.
 */
@Entity(name = "rul_translation")
@Table
public class RulTranslation {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer translationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(insertable = false, updatable = false)
    private Integer packageId;

    /** Value of {@link TranslationEntityType}. */
    @Column(length = 50, nullable = false)
    private String entityType;

    @Column(length = 100, nullable = false)
    private String entityCode;

    @Column(length = 30, nullable = false)
    private String field;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysLanguage.class)
    @JoinColumn(name = "languageId", nullable = false)
    private SysLanguage language;

    @Column(insertable = false, updatable = false)
    private Integer languageId;

    @Column(length = Length.LONG, nullable = false)
    private String textValue;

    /** Hash of the source text the translation was made from, null when it has none. */
    @Column(length = 20)
    private String sourceHash;

    public Integer getTranslationId() {
        return translationId;
    }

    public void setTranslationId(Integer translationId) {
        this.translationId = translationId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public SysLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SysLanguage language) {
        this.language = language;
        this.languageId = language != null ? language.getLanguageId() : null;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }
}
