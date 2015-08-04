package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.ax.IdObject;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_arrangement_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrangementType extends EntityBase implements IdObject<Integer> {

    @Id
    @GeneratedValue
    private Integer arrangementTypeId;

    @Column(length = 5, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    public void setArrangementTypeId(final Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ArrangementType pk=" + arrangementTypeId;
    }

    @Override
    public Integer getId() {
        return arrangementTypeId;
    }
}
