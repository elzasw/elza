package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.vo.AccordionNodeVO;
import cz.tacr.elza.controller.vo.TreeNodeVO;

import java.util.Collection;

/**
 * Třída pro výsledná data JP.
 *
 * @since 07.03.2018
 */
public class NodeData {

    private ArrangementController.DescFormDataNewVO formData;             // data formuláře požadované JP
    private Collection<TreeNodeVO> parents;     // požadovaní rodiče ke kořeni
    private Collection<TreeNodeVO> children;    // požadovaní přímí potomci
    private Collection<AccordionNodeVO> siblings; // požadovaní sourozenci
    private Integer nodeIndex;                      // index požadované JP
    private Integer nodeCount;                      // celkový počet JP v úrovni požadované JP

    public void setFormData(final ArrangementController.DescFormDataNewVO formData) {
        this.formData = formData;
    }

    public ArrangementController.DescFormDataNewVO getFormData() {
        return formData;
    }

    public void setParents(final Collection<TreeNodeVO> parents) {
        this.parents = parents;
    }

    public Collection<TreeNodeVO> getParents() {
        return parents;
    }

    public void setChildren(final Collection<TreeNodeVO> children) {
        this.children = children;
    }

    public Collection<TreeNodeVO> getChildren() {
        return children;
    }

    public Collection<AccordionNodeVO> getSiblings() {
        return siblings;
    }

    public void setSiblings(final Collection<AccordionNodeVO> siblings) {
        this.siblings = siblings;
    }

    public Integer getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(final Integer nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(final Integer nodeCount) {
        this.nodeCount = nodeCount;
    }
}
