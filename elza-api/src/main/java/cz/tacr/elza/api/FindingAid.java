package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface FindingAid extends Serializable {

    Integer getFindingAidId();

    void setFindingAidId(Integer findingAidId);

    String getName();

    void setName(String name);

    LocalDateTime getCreateDate();

    void setCreateDate(LocalDateTime createDate);
}
