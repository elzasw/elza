package cz.tacr.elza.print;

// TODO - JavaDoc - Lebeda
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 27.6.16
 */
public class RecordType {
    private String name;
    private String code;
    private Integer countRecords;
    private Integer countDirectRecords;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getCountDirectRecords() {
        return countDirectRecords;
    }

    public void setCountDirectRecords(Integer countDirectRecords) {
        this.countDirectRecords = countDirectRecords;
    }

    public Integer getCountRecords() {
        return countRecords;
    }

    public void setCountRecords(Integer countRecords) {
        this.countRecords = countRecords;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
