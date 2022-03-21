package cz.tacr.elza.controller.vo;

public class MapLayerVO {

    private String name;

    private String url;

    private String layer;

    private Boolean initial;

    private LayerType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public Boolean getInitial() {
        return initial;
    }

    public void setInitial(Boolean initial) {
        this.initial = initial;
    }

    public LayerType getType() {
        return type;
    }

    public void setType(LayerType type) {
        this.type = type;
    }
}
