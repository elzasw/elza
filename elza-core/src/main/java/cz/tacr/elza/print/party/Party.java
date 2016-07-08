package cz.tacr.elza.print.party;

import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.UnitDateText;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Party {

    private PartyName preferredName;
    private List<PartyName> names = new ArrayList<>();
    private String history;
    private String sourceInformation;
    private String characteristics;
    private Record record;
    private UnitDateText unitdateFrom;
    private UnitDateText unitdateTo;
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

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public List<PartyName> getNames() {
        return names;
    }

    public void setNames(List<PartyName> names) {
        this.names = names;
    }

    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(PartyName preferredName) {
        this.preferredName = preferredName;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public String getSourceInformation() {
        return sourceInformation;
    }

    public void setSourceInformation(String sourceInformation) {
        this.sourceInformation = sourceInformation;
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

    public UnitDateText getUnitdateFrom() {
        return unitdateFrom;
    }

    public void setUnitdateFrom(UnitDateText unitdateFrom) {
        this.unitdateFrom = unitdateFrom;
    }

    public UnitDateText getUnitdateTo() {
        return unitdateTo;
    }

    public void setUnitdateTo(UnitDateText unitdateTo) {
        this.unitdateTo = unitdateTo;
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
