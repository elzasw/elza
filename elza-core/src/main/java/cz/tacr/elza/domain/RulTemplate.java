package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.6.2016
 */
@Entity(name = "rul_template")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulTemplate {

    @Id
    @GeneratedValue
    private Integer templateId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Engine engine;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String directory;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String mimeType;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String extension;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(nullable = false)
    private Boolean deleted;

    /* Konstanty pro vazby a fieldy. */
    public static final String NAME = "name";

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Integer templateId) {
        this.templateId = templateId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(final Engine engine) {
        this.engine = engine;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    /**
     * @return balíček
     */
    public RulPackage getPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    /**
     * @return typ outputu
     */
    public RulOutputType getOutputType() {
        return outputType;
    }

    /**
     * @param outputType typ outputu
     */
    public void setOutputType(final RulOutputType outputType) {
        this.outputType = outputType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.RulTemplate)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.RulTemplate other = (cz.tacr.elza.domain.RulTemplate) obj;

        return new EqualsBuilder().append(templateId, other.getTemplateId()).isEquals();
    }

    /**
     * Výčet template enginů
     */
    public enum Engine {
        JASPER,
        FREEMARKER,
        DOCX,
        DEXML
    }
}
