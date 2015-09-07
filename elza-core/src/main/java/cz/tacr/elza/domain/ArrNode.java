package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;


/**
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Entity(name = "arr_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNode extends AbstractVersionableEntity implements IdObject<Integer>, cz.tacr.elza.api.ArrNode {

    @Id
    @GeneratedValue
    private Integer nodeId;

    @Column(nullable = true)
    private LocalDateTime lastUpdate;

    @Override
    @JsonIgnore
    public Integer getId() {
        return nodeId;
    }

    @Override
    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
