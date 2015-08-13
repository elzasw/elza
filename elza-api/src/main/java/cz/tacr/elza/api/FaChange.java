package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface FaChange extends Serializable {

    Integer getChangeId();

    void setChangeId(Integer changeId);

    LocalDateTime getChangeDate();

    void setChangeDate(LocalDateTime changeDate);
}
