package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface ArrFaChange extends Serializable {

    Integer getFaChangeId();

    void setFaChangeId(Integer changeId);

    LocalDateTime getChangeDate();

    void setChangeDate(LocalDateTime changeDate);
}
