package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrNodeOutput}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_node_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNodeOutput implements cz.tacr.elza.api.ArrNodeOutput<ArrOutput, ArrChange, ArrNode> {

    @Id
    @GeneratedValue
    private Integer nodeOutputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputId", nullable = false)
    private ArrOutput output;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    @Override
    public Integer getNodeOutputId() {
        return nodeOutputId;
    }

    @Override
    public void setNodeOutputId(final Integer nodeOutputId) {
        this.nodeOutputId = nodeOutputId;
    }

    @Override
    public ArrOutput getOutput() {
        return output;
    }

    @Override
    public void setOutput(final ArrOutput output) {
        this.output = output;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }
}
