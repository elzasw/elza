package cz.tacr.elza.controller.vo;

public class SingleSignOnEntityVO {

    private String name;
    private String url;

    public SingleSignOnEntityVO(final String name, final String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}
