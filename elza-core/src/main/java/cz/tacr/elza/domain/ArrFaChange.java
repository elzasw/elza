package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * Seznam provedených změn v archivních pomůckách.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_change")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFaChange implements cz.tacr.elza.api.ArrFaChange {

    @Id
    @GeneratedValue
    private Integer faChangeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    @Override
    public Integer getFaChangeId() {
        return faChangeId;
    }

    @Override
    public void setFaChangeId(Integer faChangeId) {
        this.faChangeId = faChangeId;
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
        return "ArrFaChange pk=" + faChangeId;
    }

}
