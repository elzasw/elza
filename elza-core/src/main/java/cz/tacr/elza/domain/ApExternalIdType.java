package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.interfaces.Versionable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.io.Serializable;

@Entity(name = "ap_external_id_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApExternalIdType implements Serializable {
    @Id
    @GeneratedValue
    private Integer externalIdType;

    @Column(length = 20, nullable = false, unique = true)
    private String code;

    @Column(length = 20, nullable = false)
    private String name;

    public Integer getExternalIdType() {
        return externalIdType;
    }

    public void setExternalIdType(Integer externalIdType) {
        this.externalIdType = externalIdType;
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
