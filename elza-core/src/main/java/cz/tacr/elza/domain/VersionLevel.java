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
@Entity(name = "arr_version_level")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VersionLevel extends EntityBase {

    @Id
    @GeneratedValue
    private Integer versionLevelId;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer levelId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Level.class)
    @JoinColumn(name = "levelId", nullable = false)
    private Level level;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer versionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Version.class)
    @JoinColumn(name = "versionId", nullable = false)
    private Version version;

    public Integer getVersionLevelId() {
        return versionLevelId;
    }

    public void setVersionLevelId(final Integer versionLevelId) {
        this.versionLevelId = versionLevelId;
    }

    public Integer getLevelId() {
        return levelId;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
        if (level != null) {
            this.levelId = level.getLevelId();
        }
    }

    public Integer getVersionId() {
        return versionId;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(final Version version) {
        this.version = version;
        if (version != null) {
            this.versionId = version.getVersionId();
        }
    }

    @Override
    public String toString() {
        return "VersionLevel pk=" + versionLevelId;
    }
}
