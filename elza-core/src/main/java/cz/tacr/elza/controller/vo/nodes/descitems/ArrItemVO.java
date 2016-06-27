package cz.tacr.elza.controller.vo.nodes.descitems;

import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Abstraktní VO hodnoty atributu.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public abstract class ArrItemVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * identifikátor hodnoty atributu
     */
    private Integer descItemObjectId;

    /**
     * pozice
     */
    private Integer position;

    /**
     * specifikace atributu
     */
    private Integer descItemSpecId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }
}