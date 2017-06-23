package cz.tacr.elza.print;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.domain.RegRegisterType;

/**
 * Class to hold Record type info
 * 
 */
public class RecordType {

    private String name;
    private String code;
    private RecordType parentType;

    private RecordType(RecordType parentType, String code, String name) {
    	this.parentType = parentType;
		this.code = code;
		this.name = name;
	}

    public RecordType getParentType() {
    	return parentType;
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

    /**
     * Return new instance of RecordType
     * @param registerType
     * @return
     */
	public static RecordType newInstance(RecordType parentType, RegRegisterType dbRegisterType) {
		RecordType recordType = new RecordType(parentType, dbRegisterType.getCode(), dbRegisterType.getName()); 
		return recordType;
	}

	/**
	 * Return existing instance of record type
	 * @param output
	 * @param dbRegisterType
	 * @return
	 */
	public static RecordType getInstance(OutputImpl output, RegRegisterType dbRegisterType) {
		String regTypeCode = dbRegisterType.getCode();
		RecordType recordType = output.getRecordType(regTypeCode);
		if (recordType == null) {
			// prepare parent
			RecordType parentType = null;
			RegRegisterType dbParentRegisterType = dbRegisterType.getParentRegisterType();
			if(dbParentRegisterType!=null) {
				parentType = getInstance(output, dbParentRegisterType);
			}
			// create new instance of record type
			recordType = RecordType.newInstance(parentType, dbRegisterType);
			// store record type
			output.addRecordType(recordType);
		}		
		return recordType;
	}
}
