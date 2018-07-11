package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.vo.DescItemValues;

import java.util.Map;


/**
 * Uzel pro filtrovaná data ve stromě.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class FilterNode {

    private ArrNodeVO node;

    private TreeNodeVO parentNode;

    /**
     * Mapa hodnot atributů na uzlu (typ atributu -> hodnota)
     */
    private Map<Integer, DescItemValues> valuesMap;

    private String[] referenceMark;

    public FilterNode() {
    }

    public FilterNode(final ArrNodeVO node, 
                      final TreeNodeVO parentNode, 
                      final Map<Integer, DescItemValues> valuesMap,
                      final String[] referenceMark) {
        this.node = node;
        this.parentNode = parentNode;
        this.valuesMap = valuesMap;
        this.referenceMark = referenceMark;
    }

    public ArrNodeVO getNode() {
        return node;
    }

    public void setNode(final ArrNodeVO node) {
        this.node = node;
    }

    public TreeNodeVO getParentNode() {
        return parentNode;
    }

    public void setParentNode(final TreeNodeVO parentNode) {
        this.parentNode = parentNode;
    }

    public Map<Integer, DescItemValues> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(final Map<Integer, DescItemValues> valuesMap) {
        this.valuesMap = valuesMap;
    }

    public String[] getReferenceMark() {
        return referenceMark;
    }

    public void setReferenceMark(final String[] referenceMark) {
        this.referenceMark = referenceMark;
    }
}
