package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class FaChange implements IdObject<Integer>, cz.tacr.elza.api.FaChange {

    @Id
    @GeneratedValue
    private Integer changeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    @Override
    public Integer getChangeId() {
      return changeId;
    }

    @Override
    public void setChangeId(Integer changeId) {
      this.changeId = changeId;
    }

    @Override
    public LocalDateTime getChangeDate() {
      return changeDate;
    }

    @Override
    public void setChangeDate(LocalDateTime changeDate) {
      this.changeDate = changeDate;
    }

    @Override
    public String toString() {
        return "FaChange pk=" + changeId;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return changeId;
    }
}
