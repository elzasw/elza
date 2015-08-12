package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_finding_aid")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class FindingAid extends EntityBase implements IdObject<Integer> {

    @Id
    @GeneratedValue
    private Integer findingAidId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createDate;

    public Integer getFindingAidId() {
        return findingAidId;
    }

    public void setFindingAidId(final Integer findingAidId) {
        this.findingAidId = findingAidId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public Integer getId() {
        return findingAidId;
    }

    @Override
    public String toString() {
        return "FindingAid pk=" + findingAidId;
    }
}
