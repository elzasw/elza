package cz.tacr.elza.controller.vo.ap;

public class ApFragmentTypeVO {

    private Integer id;

    private String code;

    private String name;

    public ApFragmentTypeVO() {
    }

    public ApFragmentTypeVO(final Integer id, final String code, final String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
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
}
