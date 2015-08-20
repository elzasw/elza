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
@Entity(name = "arr_finding_aid")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFindingAid implements IdObject<Integer>, cz.tacr.elza.api.ArrFindingAid, Serializable {

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
    @JsonIgnore
    public Integer getId() {
        return findingAidId;
    }

    @Override
    public String toString() {
        return "FindingAid pk=" + findingAidId;
    }
}
