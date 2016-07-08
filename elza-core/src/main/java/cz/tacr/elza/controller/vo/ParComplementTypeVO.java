package cz.tacr.elza.controller.vo;

/**
 * VO Číselníku typů doplňků jmen osob.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParComplementTypeVO {

    /**
     * Id.
     */
    private Integer complementTypeId;
    /**
     * Kód typu.
     */
    private String code;
    /**
     * Název typu.
     */
    private String name;

    /**
     * Pořadí zobrazení.
     */
    private Integer viewOrder;

    public Integer getComplementTypeId() {
        return complementTypeId;
    }

    public void setComplementTypeId(final Integer complementTypeId) {
        this.complementTypeId = complementTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }
}
