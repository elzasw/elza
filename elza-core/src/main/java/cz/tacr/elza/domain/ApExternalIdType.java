package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "ap_external_id_type")
public class ApExternalIdType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer externalIdTypeId;

    @Column(length = StringLength.LENGTH_20, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_20, nullable = false)
    private String name;

    public Integer getExternalIdTypeId() {
        return externalIdTypeId;
    }

    public void setExternalIdTypeId(Integer externalIdTypeId) {
        this.externalIdTypeId = externalIdTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
