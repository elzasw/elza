package cz.tacr.elza.ax;

import com.vaadin.data.util.converter.Converter;

import java.util.Locale;

public class StringToConverter implements Converter<String, Object> {

    @Override
    public Object convertToModel(String value, Class<?> targetType, Locale locale) throws ConversionException {
        return value;
    }

    @Override
    public String convertToPresentation(Object value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        return value.toString();
    }

    @Override
    public Class<Object> getModelType() {
        return Object.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }

}
