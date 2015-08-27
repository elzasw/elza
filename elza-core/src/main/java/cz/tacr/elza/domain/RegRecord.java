package cz.tacr.elza.domain;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;


/**
 * Záznamy v rejstříku.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRecord extends AbstractVersionableEntity implements IdObject<Integer>, cz.tacr.elza.api.RegRecord<RegRegisterType, RegExternalSource> {

    @Id
    @GeneratedValue
    private Integer recordId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = false)
    private RegRegisterType registerType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegExternalSource.class)
    @JoinColumn(name = "externalSourceId")
    private RegExternalSource externalSource;

    @Column(length = 500, nullable = false)
    private String record;

    @Column(nullable = false)
    private String characteristics;

    @Column()
    private String comment;

    @Column(nullable = false)
    private Boolean local;

    @Column(length = 200)
    private String external_id;


    @Override
    public Integer getRecordId() {
        return recordId;
    }

    @Override
    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    @Override
    public RegRegisterType getRegisterType() {
        return registerType;
    }

    @Override
    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    @Override
    public RegExternalSource getExternalSource() {
        return externalSource;
    }

    @Override
    public void setExternalSource(final RegExternalSource externalSource) {
        this.externalSource = externalSource;
    }

    @Override
    public String getRecord() {
        return record;
    }

    @Override
    public void setRecord(final String record) {
        this.record = record;
    }

    @Override
    public String getCharacteristics() {
        return characteristics;
    }

    @Override
    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(final String comment) {
        this.comment = comment;
    }

    @Override
    public Boolean getLocal() {
        return local;
    }

    @Override
    public void setLocal(final Boolean local) {
        this.local = local;
    }

    @Override
    public String getExternal_id() {
        return external_id;
    }

    @Override
    public void setExternal_id(final String external_id) {
        this.external_id = external_id;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return recordId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartySubtype)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RegRecord other = (RegRecord) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
