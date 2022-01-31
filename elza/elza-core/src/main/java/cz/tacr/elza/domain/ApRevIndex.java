package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "ap_rev_index")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApRevIndex {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer indexId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevPart.class)
    @JoinColumn(name = "part_id", nullable = false)
    private ApRevPart part;

    @Column(name = "part_id", nullable = false, updatable = false, insertable = false)
    private Integer partId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String indexType;

    @Column(length = StringLength.LENGTH_4000, nullable = false)
    private String value;

    public Integer getIndexId() {
        return indexId;
    }

    public void setIndexId(Integer indexId) {
        this.indexId = indexId;
    }

    public ApRevPart getPart() {
        return part;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPart(ApRevPart part) {
        this.part = part;
        this.partId = part != null ? part.getPartId() : null;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
