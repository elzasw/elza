package cz.tacr.elza.controller.vo;

/**
 * Zjednodušené VO rejstříku k reprezentaci nadřízených rejstříků.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 1. 2016
 */
public class RegRecordParentVO {

    private Integer id;

    private String record;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }
}
