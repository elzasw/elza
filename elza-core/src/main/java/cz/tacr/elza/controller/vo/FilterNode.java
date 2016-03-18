package cz.tacr.elza.controller.vo;

import java.util.Map;


/**
 * Uzel pro filtrovaná data ve stromě.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class FilterNode {

    /**
     * Id uzlu.
     */
    private Integer id;

    /**
     * Mapa hodnot atributů na uzlu (typ atributu -> hodnota)
     */
    private Map<Integer, String> valuesMap;

    public FilterNode() {
    }

    public FilterNode(final Integer id, final Map<Integer, String> valuesMap) {
        this.id = id;
        this.valuesMap = valuesMap;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Map<Integer, String> getValuesMap() {
        return valuesMap;
    }

    public void setValuesMap(final Map<Integer, String> valuesMap) {
        this.valuesMap = valuesMap;
    }
}
