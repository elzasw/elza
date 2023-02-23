
package cz.tacr.elza.schema.support;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;



public class LongAdapter extends XmlAdapter<String, Long> {

    public Long unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        return value != null ? javax.xml.bind.DatatypeConverter.parseLong(value) : null;
    }

    public String marshal(Long value) {
        return value != null ? javax.xml.bind.DatatypeConverter.printLong(value.longValue()) : null;
    }
}
