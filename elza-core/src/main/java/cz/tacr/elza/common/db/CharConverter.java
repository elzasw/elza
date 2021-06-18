package cz.tacr.elza.common.db;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class CharConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String value) {
        return value;
    }

    @Override
    public String convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

}
