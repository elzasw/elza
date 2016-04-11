package cz.tacr.elza.controller.vo;

import java.util.Map;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.vo.DescItemValue;
import cz.tacr.elza.domain.vo.DescItemValues;


/**
 * Uzel pro filtrovaná data ve stromě.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class FilterNode {


    private ArrNodeVO node;
    private ArrNodeVO parentNode;

    /**
     * Mapa hodnot atributů na uzlu (typ atributu -> hodnota)
     */
    private Map<Integer, DescItemValues> valuesMap;

    public FilterNode() {
    }

    public FilterNode(final ArrNodeVO node, final ArrNodeVO parentNode, final Map<Integer, DescItemValues> valuesMap) {
        this.node = node;
        this.parentNode = parentNode;
        this.valuesMap = valuesMap;
    }

    public ArrNodeVO getNode() {
        return node;
    }

    public void setNode(final ArrNodeVO node) {
        this.node = node;
    }

    public ArrNodeVO getParentNode() {
        return parentNode;
    }

    public void setParentNode(final ArrNodeVO parentNode) {
        this.parentNode = parentNode;
    }

    public Map<Integer, DescItemValues> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(final Map<Integer, DescItemValues> valuesMap) {
        this.valuesMap = valuesMap;
    }
}
