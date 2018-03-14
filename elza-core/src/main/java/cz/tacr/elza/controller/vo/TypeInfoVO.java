package cz.tacr.elza.controller.vo;

/**
 * VO pro informace o typu atributu.
 *
 * @since 06.03.2018
 */
public class TypeInfoVO {

    private Integer id;

    private Integer width;

    public TypeInfoVO(final Integer id, final Integer width) {
        this.id = id;
        this.width = width;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }
}
