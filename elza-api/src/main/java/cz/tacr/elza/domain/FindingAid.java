package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_finding_aid")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FindingAid extends EntityBase {

    @Id
    @GeneratedValue
    private Integer findigAidId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createDate;

    public Integer getFindigAidId() {
        return findigAidId;
    }

    public void setFindigAidId(final Integer findigAidId) {
        this.findigAidId = findigAidId;
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
    public String toString() {
        return "FindingAid pk=" + findigAidId;
    }
}
