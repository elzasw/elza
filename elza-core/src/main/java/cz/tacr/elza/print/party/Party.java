package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.print.Record;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public abstract class Party {

    private PartyName preferredName;
    private List<PartyName> names = new ArrayList<>();
    private String history;
    private String sourceInformation;
    private String characteristics;
    private Record record;
    private String type;
    private String typeCode;

    /**
     * @return vrací hodnotu formátovanou jako text k tisku
     */
    public String serialize() {
        return (StringUtils.defaultString(record.getRecord()) + " " + StringUtils.defaultString(record.getCharacteristics())).trim();
    }

    /**
     * @return obsah položky record.getRecord()
     */
    public String getName() {
        return record.getRecord();
    }


    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(final String history) {
        this.history = history;
    }

    public List<PartyName> getNames() {
        return names;
    }

    public void setNames(final List<PartyName> names) {
        this.names = names;
    }

    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final PartyName preferredName) {
        this.preferredName = preferredName;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(final Record record) {
        this.record = record;
    }

    public String getSourceInformation() {
        return sourceInformation;
    }

    public void setSourceInformation(final String sourceInformation) {
        this.sourceInformation = sourceInformation;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
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
