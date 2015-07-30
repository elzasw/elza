package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "RUL_ARRANGEMENT_TYPE")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrangementType extends EntityBase {

    @Id
    @GeneratedValue
    private Integer arrangementTypeId;

    @Column(length = 50, nullable = false)
    private String name;

    public Integer getArrangementTypeId() {
      return arrangementTypeId;
    }

    public void setArrangementTypeId(Integer arrangementTypeId) {
      this.arrangementTypeId = arrangementTypeId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
        return "ArrangementType pk=" + arrangementTypeId;
    }
}
