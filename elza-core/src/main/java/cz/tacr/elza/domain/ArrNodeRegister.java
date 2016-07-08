package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Přiřazení rejstříkových hesel k jednotce archivního popisu.
 */
@Entity(name = "arr_node_register")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNodeRegister implements cz.tacr.elza.api.ArrNodeRegister<ArrNode, RegRecord, ArrChange> {

    @Id
    @GeneratedValue
    private Integer nodeRegisterId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;


    @Override
    public Integer getNodeRegisterId() {
        return nodeRegisterId;
    }

    @Override
    public void setNodeRegisterId(Integer nodeRegisterId) {
        this.nodeRegisterId = nodeRegisterId;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(ArrNode node) {
        this.node = node;
    }

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(RegRecord record) {
        this.record = record;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    @Override
    public String toString() {
        return "ArrNodeRegister pk=" + nodeRegisterId;
    }
}
