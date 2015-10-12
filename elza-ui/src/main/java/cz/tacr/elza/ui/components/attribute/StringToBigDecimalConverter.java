package cz.tacr.elza.ui.components.attribute;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.data.util.converter.AbstractStringToNumberConverter;


/**
 * Konverter pro správné zobrazování decimálních čísel.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
public class StringToBigDecimalConverter extends AbstractStringToNumberConverter<BigDecimal> {

    @Override
    protected NumberFormat getFormat(Locale locale) {
        NumberFormat numberFormat = super.getFormat(locale);
        if (numberFormat instanceof DecimalFormat) {
            DecimalFormat decimalFormat = ((DecimalFormat) numberFormat);
            decimalFormat.setMinimumFractionDigits(0);
            decimalFormat.setMaximumFractionDigits(20);
            decimalFormat.setParseBigDecimal(true);
        }

        return numberFormat;
    }

    @Override
    public BigDecimal convertToModel(String value,
                                     Class<? extends BigDecimal> targetType, Locale locale)
            throws com.vaadin.data.util.converter.Converter.ConversionException {
        return (BigDecimal) convertToNumber(value, BigDecimal.class, locale);
    }

    @Override
    public Class<BigDecimal> getModelType() {
        return BigDecimal.class;
    }
}