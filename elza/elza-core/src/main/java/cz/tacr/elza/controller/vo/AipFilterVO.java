package cz.tacr.elza.controller.vo;

public class AipFilterVO {
    private String id;
    private String attr;
    private String value;
    private AipFilterCriteria criteria;
    private String from;
    private String to;
    private String path;

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AipFilterCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(AipFilterCriteria criteria) {
        this.criteria = criteria;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

