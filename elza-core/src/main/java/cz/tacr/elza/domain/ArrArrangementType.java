package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_arrangement_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrArrangementType implements cz.tacr.elza.api.ArrArrangementType {

    @Id
    @GeneratedValue
    private Integer arrangementTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Override
    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    @Override
    public void setArrangementTypeId(final Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ArrArrangementType pk=" + arrangementTypeId;
    }

}
