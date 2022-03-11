package cz.tacr.elza.controller.vo;

public class MapLayerVO {

    private String name;

    private String url;

    private Integer zIndex;

    private String layer;

    private Boolean initial;

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

    public Integer getzIndex() {
        return zIndex;
    }

    public void setzIndex(Integer zIndex) {
        this.zIndex = zIndex;
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
}
