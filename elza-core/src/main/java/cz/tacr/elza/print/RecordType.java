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
    private int countRecords = 0;
    private int countDirectRecords = 0;

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

    public void setCode(final String code) {
        this.code = code;
    }

    public int getCountDirectRecords() {
        return countDirectRecords;
    }

    public void setCountDirectRecords(final int countDirectRecords) {
        this.countDirectRecords = countDirectRecords;
    }

    public int getCountRecords() {
        return countRecords;
    }

    public void setCountRecords(final int countRecords) {
        this.countRecords = countRecords;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
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
