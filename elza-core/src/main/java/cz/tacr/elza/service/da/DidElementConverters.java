package cz.tacr.elza.service.da;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.archivists.ead3.schema.Abstract;
import org.archivists.ead3.schema.Unitdatestructured;
import org.archivists.ead3.schema.Unittitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Converts the elements of {@code <did>} of the inherent archival description to the values of
 * the items of the digital entities.
 *
 * Which item type and specification an element becomes is decided by the IMPORT_DA script of
 * the rules, from the class of the element and its local type; how its content becomes a value
 * is decided here, by the converter registered for the class of the element. An element without
 * a converter is not taken over. Taking over a new element is a new {@link #register} call - and
 * a new branch of the script, which names its item type.
 */
final class DidElementConverters {

    private static final Logger logger = LoggerFactory.getLogger(DidElementConverters.class);

    /** Value of {@code altrender} marking a value repeated from a higher level. */
    private static final String INHERITED = "inherited";

    /** What the rules map an element to. */
    record Mapping(RulItemType itemType, @Nullable RulItemSpec itemSpec) {
    }

    /**
     * Converts one element to the value of an item.
     *
     * @param <T> class of the element
     */
    @FunctionalInterface
    interface Converter<T> {

        /**
         * @return the value, without its data type set; null when the element carries none
         * @throws EadContentException when the content is not written the way it can be read
         */
        @Nullable
        ArrData convert(T element, Mapping mapping);
    }

    /**
     * How the elements of one class are read.
     *
     * @param localType the kind of the element within its class, as the profile writes it;
     *            handed to the script together with the class
     * @param altrender the attribute that marks an inherited value
     */
    private record ElementType<T>(Function<T, String> localType, Function<T, String> altrender,
                                  Converter<T> converter) {
    }

    private static final Map<Class<?>, ElementType<?>> ELEMENT_TYPES = new HashMap<>();

    static {
        register(Unittitle.class, Unittitle::getLocaltype, Unittitle::getAltrender,
                 (unittitle, mapping) -> textValue(unittitle.getContent(), mapping.itemType()));
        register(Abstract.class, Abstract::getLocaltype, Abstract::getAltrender,
                 (abs, mapping) -> textValue(abs.getContent(), mapping.itemType()));
        // The kind of date (date of origin, date of content, ...) is the local type of <daterange>.
        register(Unitdatestructured.class,
                 unitdate -> unitdate.getDaterange() != null ? unitdate.getDaterange().getLocaltype() : null,
                 Unitdatestructured::getAltrender,
                 DidElementConverters::unitdateValue);
    }

    private DidElementConverters() {
    }

    private static <T> void register(Class<T> elementClass, Function<T, String> localType,
                                     Function<T, String> altrender, Converter<T> converter) {
        ELEMENT_TYPES.put(elementClass, new ElementType<>(localType, altrender, converter));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static ElementType<Object> elementType(Object element) {
        return (ElementType<Object>) ELEMENT_TYPES.get(element.getClass());
    }

    /**
     * @return whether the element has a converter, so that it is worth asking the script for
     *         its item type
     */
    static boolean isSupported(Object element) {
        return ELEMENT_TYPES.containsKey(element.getClass());
    }

    /**
     * An inherited value only repeats the value of a higher level, where it is taken over
     * already - taking it over again would attach it to every component below.
     *
     * @return whether the supported element is marked as inherited
     */
    static boolean isInherited(Object element) {
        ElementType<Object> type = elementType(element);
        return type != null && INHERITED.equalsIgnoreCase(StringUtils.trim(type.altrender().apply(element)));
    }

    /**
     * @return local type of the supported element; null when it has none
     */
    @Nullable
    static String localType(Object element) {
        ElementType<Object> type = elementType(element);
        return type == null ? null : StringUtils.trimToNull(type.localType().apply(element));
    }

    /**
     * @return the value with its data type set; null when the element carries none
     * @throws EadContentException when the content is not written the way it can be read
     * @throws SystemException when the item type cannot hold what the element carries - the
     *             rules map the element wrongly
     */
    @Nullable
    static ArrData convert(Object element, Mapping mapping) {
        ElementType<Object> type = elementType(element);
        if (type == null) {
            return null;
        }
        ArrData data = type.converter().convert(element, mapping);
        if (data != null) {
            data.setDataType(DataType.fromId(mapping.itemType().getDataTypeId()).getEntity());
        }
        return data;
    }

    @Nullable
    private static ArrData unitdateValue(Unitdatestructured unitdate, Mapping mapping) {
        requireDataType(mapping.itemType(), DataType.UNITDATE);
        // A date of a particular kind is told apart from the date of origin only by the
        // specification; rules that name none do not know the kind, and taking the date over
        // would misread it as the date of origin.
        String localType = localType(unitdate);
        if (localType != null && mapping.itemSpec() == null) {
            logger.warn("Datace s localtype={} se nepřebírá, pravidla pro ni v prvku popisu {} neurčují specifikaci",
                        localType, mapping.itemType().getCode());
            return null;
        }
        return EadUnitdateParser.parse(unitdate);
    }

    /**
     * Text of an element with mixed content. Only its own text is taken; text inside nested
     * formatting elements is not read.
     */
    @Nullable
    private static ArrData textValue(List<Serializable> content, RulItemType itemType) {
        StringBuilder sb = new StringBuilder();
        for (Serializable part : content) {
            if (part instanceof String s) {
                sb.append(s);
            }
        }
        if (StringUtils.isBlank(sb)) {
            return null;
        }
        DataType dataType = requireDataType(itemType, DataType.STRING, DataType.TEXT);
        if (dataType == DataType.TEXT) {
            return new ArrDataText(sb.toString().strip());
        }
        // A string is a single line; its line breaks and indentation are those of the XML.
        String text = StringUtils.normalizeSpace(sb.toString());
        if (text.length() > StringLength.LENGTH_1000) {
            throw new EadContentException("text je delší než " + StringLength.LENGTH_1000
                    + " znaků, které pojme prvek popisu " + itemType.getCode());
        }
        return new ArrDataString(text);
    }

    private static DataType requireDataType(RulItemType itemType, DataType... allowed) {
        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        for (DataType a : allowed) {
            if (a == dataType) {
                return dataType;
            }
        }
        throw new SystemException("Prvek popisu " + itemType.getCode() + " má datový typ " + dataType
                + ", do kterého nelze převzít hodnotu z EAD; skript IMPORT_DA jej přiřazuje nesprávně",
                BaseCode.INVALID_STATE);
    }
}
