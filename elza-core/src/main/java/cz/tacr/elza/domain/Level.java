package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_level")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"treeId"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Level extends EntityBase {

    @Id
    @GeneratedValue
    private Integer levelId;

    @Column(nullable = false)
    private Integer treeId;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer parentTreeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Level.class)
    @JoinColumn(name = "parentTreeId", nullable = false)
    private Level parent;

    @Column(nullable = false)
    private Integer position;

    public Integer getLevelId() {
        return levelId;
    }

    public void setLevelId(final Integer levelId) {
        this.levelId = levelId;
    }

    public Integer getTreeId() {
        return treeId;
    }

    public void setTreeId(final Integer treeId) {
        this.treeId = treeId;
    }

    public Integer getParentTreeId() {
        return parentTreeId;
    }

    public Level getParent() {
        return parent;
    }

    public void setParent(final Level parent) {
        this.parent = parent;
        if (parent != null) {
            parentTreeId = parent.getTreeId();
        }
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Level pk=" + levelId;
    }
}
