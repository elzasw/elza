package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vazba výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_node_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNodeOutput implements Serializable {

    @Id
    @GeneratedValue
    private Integer nodeOutputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    /**
     * @return  identifikátor entity
     */
    public Integer getNodeOutputId() {
        return nodeOutputId;
    }

    /**
     * @param nodeOutputId identifikátor entity
     */
    public void setNodeOutputId(final Integer nodeOutputId) {
        this.nodeOutputId = nodeOutputId;
    }

    /**
     * @return pojmenovaný výstup z archivního souboru
     */
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    /**
     * @param outputDefinition pojmenovaný výstup z archivního souboru
     */
    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    /**
     * @return navázaný uzel
     */
    public ArrNode getNode() {
        return node;
    }

    /**
     * @param node navázaný uzel
     */
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    /**
     * @return změna vytvoření
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange změna vytvoření
     */
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return změna smazání
     */
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    /**
     * @param deleteChange změna smazání
     */
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }
}
