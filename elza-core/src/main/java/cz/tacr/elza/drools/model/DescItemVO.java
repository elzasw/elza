package cz.tacr.elza.drools.model;

import java.util.List;


/**
 * Value object pro hodnotu atributu.
 * Obsahuje pouze typ atributu a typ zm�ny.
 *
 * @author Martin �lapa
 * @since 27.11.2015
 */
public class DescItemVO {

    /**
     * Id atributu.
     */
    private Integer descItemId;

    /**
     * Typ atributu
     */
    private String type;
    /**
     * Specifikace.
     */
    private String specCode;

    /**
     * Obal.
     */
    private VOPacket packet;


    /**
     * Typ zm�ny atributu.
     */
    private DescItemChange change;

    public Integer getDescItemId() {
        return descItemId;
    }

    public void setDescItemId(final Integer descItemId) {
        this.descItemId = descItemId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public DescItemChange getChange() {
        return change;
    }

    public void setChange(final DescItemChange change) {
        this.change = change;
    }

    public String getSpecCode() {
        return specCode;
    }

    public void setSpecCode(final String specCode) {
        this.specCode = specCode;
    }

    public VOPacket getPacket() {
        return packet;
    }

    public void setPacket(final VOPacket packet) {
        this.packet = packet;
    }
}
