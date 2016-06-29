package cz.tacr.elza.print;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.StringJoiner;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Packet {
    private String type;
    private String typeCode;
    private String typeShortcut;
    private String storageNumber;
    private String state;

    // TODO - JavaDoc - Lebeda
    public String serialize() {
        StringJoiner sj = new StringJoiner(", ");
        if (StringUtils.isNotBlank(type)) {
            sj.add(type);
        }
        if (StringUtils.isNotBlank(typeCode)) {
            sj.add(typeCode);
        }
        if (StringUtils.isNotBlank(typeShortcut)) {
            sj.add(typeShortcut);
        }
        if (StringUtils.isNotBlank(storageNumber)) {
            sj.add(storageNumber);
        }
        if (StringUtils.isNotBlank(state)) {
            sj.add(state);
        }
        return sj.toString();
    }

    // TODO Lebeda - implementovat ???
//    +getNodes() : Node[*] ???


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeShortcut() {
        return typeShortcut;
    }

    public void setTypeShortcut(String typeShortcut) {
        this.typeShortcut = typeShortcut;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
