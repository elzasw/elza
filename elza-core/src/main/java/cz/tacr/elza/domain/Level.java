package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "ARR_LEVEL")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Level extends EntityBase {

    @Id
    @GeneratedValue
    private Integer levelId;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer versionLevelId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = VersionLevel.class)
    @JoinColumn(name = "versionLevelId", nullable = false)
    private VersionLevel versionLevel;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer parentVersionLevelId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = VersionLevel.class)
    @JoinColumn(name = "parentVersionLevelId", nullable = false)
    private VersionLevel parentVersionLevel;

    @Column(nullable = false)
    private Integer position;

    public Integer getLevelId() {
      return levelId;
    }

    public void setLevelId(Integer levelId) {
      this.levelId = levelId;
    }

    public Integer getVersionLevelId() {
      return versionLevelId;
    }

    public void setVersionLevelId(Integer versionLevelId) {
      this.versionLevelId = versionLevelId;
    }

    public VersionLevel getVersionLevel() {
      return versionLevel;
    }

    public void setVersionLevel(VersionLevel versionLevel) {
      this.versionLevel = versionLevel;
    }

    public Integer getParentVersionLevelId() {
      return parentVersionLevelId;
    }

    public void setParentVersionLevelId(Integer parentVersionLevelId) {
      this.parentVersionLevelId = parentVersionLevelId;
    }

    public VersionLevel getParentVersionLevel() {
      return parentVersionLevel;
    }

    public void setParentVersionLevel(VersionLevel parentVersionLevel) {
      this.parentVersionLevel = parentVersionLevel;
    }

    public Integer getPosition() {
      return position;
    }

    public void setPosition(Integer position) {
      this.position = position;
    }

    @Override
    public String toString() {
        return "Level pk=" + levelId;
    }
}
