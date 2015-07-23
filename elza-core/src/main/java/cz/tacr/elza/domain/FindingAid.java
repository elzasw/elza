package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(/*name = "FA_FINDING_AID"*/)
public class FindingAid extends EntityBase {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer findigAidId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentLocalDateTime")
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
