package cz.tacr.elza.controller.vo;

/**
 * VO pro reprezentaci závislosti.
 *
 * @since 15.09.2017
 */
public class PackageDependencyVO {

    private String code;

    private Integer version;

    public PackageDependencyVO() {
    }

    public PackageDependencyVO(final String code, final Integer version) {
        this.code = code;
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
