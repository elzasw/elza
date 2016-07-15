package cz.tacr.elza.print;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 27.6.16
 */
public class RecordType {
    private String name;
    private String code;
    private Integer countRecords = 0;
    private Integer countDirectRecords = 0;

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public RecordType getRecordType() {
        return this;
    }

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

    @Override
    public boolean equals(final Object o) {
        if (o instanceof RecordType) {
            RecordType other = (RecordType) o;
            return new EqualsBuilder().append(getCode(), other.getCode()).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getCode()).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
