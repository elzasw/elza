package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFindingAidVersion}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_finding_aid")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFindingAid extends AbstractVersionableEntity implements cz.tacr.elza.api.ArrFindingAid, Serializable {

    @Id
    @GeneratedValue
    private Integer findingAidId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createDate;

    @Override
    public Integer getFindingAidId() {
        return findingAidId;
    }

    @Override
    public void setFindingAidId(final Integer findingAidId) {
        this.findingAidId = findingAidId;
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
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    @Override
    public void setCreateDate(final LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "FindingAid pk=" + findingAidId;
    }
}
