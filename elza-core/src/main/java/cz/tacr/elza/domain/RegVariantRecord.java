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

import cz.tacr.elza.api.RegScope;
import cz.tacr.elza.api.Versionable;
import cz.tacr.elza.api.interfaces.IRegScope;
import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Variantní rejstříková hesla.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_variant_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegVariantRecord extends AbstractVersionableEntity implements  cz.tacr.elza.api.RegVariantRecord<RegRecord>, Versionable, Serializable, IRegScope {

    @Id
    @GeneratedValue
    private Integer variantRecordId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord regRecord;

//    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
//    @JoinColumn(name = "recordId", nullable = false, insertable = false, updatable = false)
//    private RegRecord regRecordPar;

    @Column(length = StringLength.LENGTH_1000)
    private String record;

    /* Konstanty pro vazby a fieldy. */
    public static final String RECORD = "record";


    @Override
    public Integer getVariantRecordId() {
        return variantRecordId;
    }

    @Override
    public void setVariantRecordId(final Integer variantRecordId) {
        this.variantRecordId = variantRecordId;
    }

    @Override
    public RegRecord getRegRecord() {
        return regRecord;
    }

    @Override
    public void setRegRecord(final RegRecord regRecord) {
        this.regRecord = regRecord;
    }

    @Override
    public String getRecord() {
        return record;
    }

    @Override
    public void setRecord(final String record) {
        this.record = record;
    }

//    @Override
//    public RegRecord getRegRecordPar() {
//        return regRecordPar;
//    }
//
//    @Override
//    public void setRegRecordPar(RegRecord regRecordPar) {
//        this.regRecordPar = regRecordPar;
//    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.RegVariantRecord)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RegVariantRecord other = (RegVariantRecord) obj;

        return new EqualsBuilder().append(variantRecordId, other.getVariantRecordId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(variantRecordId).toHashCode();
    }

    @Override
    public String toString() {
        return "RegVariantRecord pk=" + variantRecordId;
    }

    @Override
    public RegScope getRegScope() {
        return regRecord.getScope();
    }
}
