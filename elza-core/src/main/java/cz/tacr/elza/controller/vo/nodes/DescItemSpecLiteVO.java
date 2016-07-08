package cz.tacr.elza.controller.vo.nodes;


/**
 * VO
 *
 * @author Martin Šlapa
 * @since 11.2.2016
 */
public class DescItemSpecLiteVO {
    /**
     * identifikator specifikace
     */
    private Integer id;

    /**
     * typ
     */
    private Integer type;

    /**
     * opakovatelnost
     */
    private Integer rep;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(final Integer type) {
        this.type = type;
    }

    public Integer getRep() {
        return rep;
    }

    public void setRep(final Integer rep) {
        this.rep = rep;
    }
}
