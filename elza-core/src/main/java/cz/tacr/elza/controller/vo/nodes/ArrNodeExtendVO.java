package cz.tacr.elza.controller.vo.nodes;

public class ArrNodeExtendVO {

    private Integer id;

    private String name;

    private String uuid;

    private String fundName;

    public ArrNodeExtendVO() {
    }

    public ArrNodeExtendVO(Integer id, String name, String uuid, String fundName) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.fundName = fundName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
