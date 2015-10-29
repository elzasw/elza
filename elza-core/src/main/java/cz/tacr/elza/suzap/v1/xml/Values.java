package cz.tacr.elza.suzap.v1.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * Obalovací třída pro hodnoty.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlType(name = "values", namespace = NamespaceInfo.NAMESPACE)
public class Values {

    /** Souřadnice. */
    @XmlElement
    @XmlElementWrapper(name = "coordinates-list")
    private List<ValueCoordinates> coordinates;

    /** Desetinná čísla. */
    @XmlElement
    private List<ValueDecimal> decimals;

    /** Formátovaný text. */
    @XmlElement
    private List<ValueFormattedText> formattedTexts;

    /** Celá čísla. */
    @XmlElement
    private List<ValueInteger> integers;

    /** Odkazy na osoby. */
    @XmlElement
    private List<ValuePartyRef> parties;

    /** Odkazy do rejstříku. */
    @XmlElement
    private List<ValueRecordRef> records;

    /** Textové řetězce. */
    @XmlElement
    private List<ValueString> strings;

    /** Texty. */
    @XmlElement
    private List<ValueText> texts;

    /** Datace. */
    @XmlElement
    private List<ValueUnitDate> unitDates;

    /** Referenční označení. */
    @XmlElement
    private List<ValueUnitId> unitIds;
}
