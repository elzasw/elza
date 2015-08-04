package cz.tacr.elza.domain;

import java.time.LocalDateTime;

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
@Entity(name = "arr_fa_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class FaChange extends EntityBase implements IdObject<Integer> {

    @Id
    @GeneratedValue
    private Integer changeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    public Integer getChangeId() {
      return changeId;
    }

    public void setChangeId(Integer changeId) {
      this.changeId = changeId;
    }

    public LocalDateTime getChangeDate() {
      return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
      this.changeDate = changeDate;
    }

    @Override
    public String toString() {
        return "FaChange pk=" + changeId;
    }

    @Override
    public Integer getId() {
        return changeId;
    }
}
