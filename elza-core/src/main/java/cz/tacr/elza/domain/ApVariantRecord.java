package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;


/**
 * Variantní rejstříkové heslo.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "ap_variant_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApVariantRecord extends AbstractVersionableEntity implements Versionable, Serializable, IApScope {

    @Id
    @GeneratedValue
    private Integer variantRecordId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private ApRecord apRecord;

    @Column(length = StringLength.LENGTH_1000)
    private String record;

    /* Konstanty pro vazby a fieldy. */
    public static final String RECORD = "record";
    public static final String RECORD_FK = "apRecord.recordId";

    /**
     * Vlastní ID.
     * @return  id var. hesla
     */
    public Integer getVariantRecordId() {
        return variantRecordId;
    }

    /**
     * Vlastní ID.
     * @param variantRecordId id var. hesla
     */
    public void setVariantRecordId(final Integer variantRecordId) {
        this.variantRecordId = variantRecordId;
    }

    /**
     * Vazba na heslo rejstříku.
     * @return  objekt hesla
     */
    public ApRecord getApRecord() {
        return apRecord;
    }

    /**
     * Vazba na heslo rejstříku.
     * @param apRecord objekt hesla
     */
    public void setApRecord(final ApRecord apRecord) {
        this.apRecord = apRecord;
    }

    /**
     * Obsah hesla.
     * @return obsah variantního hesla
     */
    public String getRecord() {
        return record;
    }

    /**
     * Obsah hesla.
     * @param record obsah variantního hesla
     */
    public void setRecord(final String record) {
        this.record = record;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ApVariantRecord)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ApVariantRecord other = (ApVariantRecord) obj;

        return new EqualsBuilder().append(variantRecordId, other.getVariantRecordId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(variantRecordId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApVariantRecord pk=" + variantRecordId;
    }

    @Override
    public ApScope getApScope() {
        return apRecord.getScope();
    }
}
