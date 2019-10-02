package cz.tacr.elza.service.vo;

/**
 * Modelový objekt pro groovy - položka popisu.
 */
public class SimpleItem {

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * Kód typu.
     */
    private String type;

    /**
     * Kód specifikace.
     */
    private String spec;

    /**
     * Pozice.
     */
    private Integer position;

    /**
     * Textová reprezentace hodnoty.
     */
    private String value;

    public SimpleItem(final Integer id,
                      final String type,
                      final String spec,
                      final Integer position,
                      final String value) {
        this.id = id;
        this.type = type;
        this.spec = spec;
        this.position = position;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSpec() {
        return spec;
    }

    public Integer getPosition() {
        return position;
    }

    public String getValue() {
        return value;
    }
}
