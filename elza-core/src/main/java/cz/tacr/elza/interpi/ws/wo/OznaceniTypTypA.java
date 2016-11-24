//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.24 at 04:42:06 PM CET 
//


package cz.tacr.elza.interpi.ws.wo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for oznaceni_typTypA.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="oznaceni_typTypA"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="úřední / skutečné / světské jméno / jméno za svobodna"/&gt;
 *     &lt;enumeration value="jméno, pod nímž je entita nejvíce známá"/&gt;
 *     &lt;enumeration value="akronym / zkratka"/&gt;
 *     &lt;enumeration value="autorská šifra"/&gt;
 *     &lt;enumeration value="pseudonym"/&gt;
 *     &lt;enumeration value="církevní jméno"/&gt;
 *     &lt;enumeration value="jméno získané sňatkem"/&gt;
 *     &lt;enumeration value="historická podoba jména"/&gt;
 *     &lt;enumeration value="přímé pořadí"/&gt;
 *     &lt;enumeration value="jméno úřední"/&gt;
 *     &lt;enumeration value="jméno preferované entitou"/&gt;
 *     &lt;enumeration value="historická / dřívější forma jména"/&gt;
 *     &lt;enumeration value="jediný známý tvar jména v daném období"/&gt;
 *     &lt;enumeration value="uměle vytvořené označení"/&gt;
 *     &lt;enumeration value="singulár"/&gt;
 *     &lt;enumeration value="plurál"/&gt;
 *     &lt;enumeration value="přejatý termín"/&gt;
 *     &lt;enumeration value="zastaralý, historický termín"/&gt;
 *     &lt;enumeration value="nevhodný termín"/&gt;
 *     &lt;enumeration value="užší termín"/&gt;
 *     &lt;enumeration value="odborný termín"/&gt;
 *     &lt;enumeration value="invertovaná podoba"/&gt;
 *     &lt;enumeration value="antonymum"/&gt;
 *     &lt;enumeration value="homonymum"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "oznaceni_typTypA")
@XmlEnum
public enum OznaceniTypTypA {

    @XmlEnumValue("\u00fa\u0159edn\u00ed / skute\u010dn\u00e9 / sv\u011btsk\u00e9 jm\u00e9no / jm\u00e9no za svobodna")
    ÚŘEDNÍ_SKUTEČNÉ_SVĚTSKÉ_JMÉNO_JMÉNO_ZA_SVOBODNA("\u00fa\u0159edn\u00ed / skute\u010dn\u00e9 / sv\u011btsk\u00e9 jm\u00e9no / jm\u00e9no za svobodna"),
    @XmlEnumValue("jm\u00e9no, pod n\u00edm\u017e je entita nejv\u00edce zn\u00e1m\u00e1")
    JMÉNO_POD_NÍMŽ_JE_ENTITA_NEJVÍCE_ZNÁMÁ("jm\u00e9no, pod n\u00edm\u017e je entita nejv\u00edce zn\u00e1m\u00e1"),
    @XmlEnumValue("akronym / zkratka")
    AKRONYM_ZKRATKA("akronym / zkratka"),
    @XmlEnumValue("autorsk\u00e1 \u0161ifra")
    AUTORSKÁ_ŠIFRA("autorsk\u00e1 \u0161ifra"),
    @XmlEnumValue("pseudonym")
    PSEUDONYM("pseudonym"),
    @XmlEnumValue("c\u00edrkevn\u00ed jm\u00e9no")
    CÍRKEVNÍ_JMÉNO("c\u00edrkevn\u00ed jm\u00e9no"),
    @XmlEnumValue("jm\u00e9no z\u00edskan\u00e9 s\u0148atkem")
    JMÉNO_ZÍSKANÉ_SŇATKEM("jm\u00e9no z\u00edskan\u00e9 s\u0148atkem"),
    @XmlEnumValue("historick\u00e1 podoba jm\u00e9na")
    HISTORICKÁ_PODOBA_JMÉNA("historick\u00e1 podoba jm\u00e9na"),
    @XmlEnumValue("p\u0159\u00edm\u00e9 po\u0159ad\u00ed")
    PŘÍMÉ_POŘADÍ("p\u0159\u00edm\u00e9 po\u0159ad\u00ed"),
    @XmlEnumValue("jm\u00e9no \u00fa\u0159edn\u00ed")
    JMÉNO_ÚŘEDNÍ("jm\u00e9no \u00fa\u0159edn\u00ed"),
    @XmlEnumValue("jm\u00e9no preferovan\u00e9 entitou")
    JMÉNO_PREFEROVANÉ_ENTITOU("jm\u00e9no preferovan\u00e9 entitou"),
    @XmlEnumValue("historick\u00e1 / d\u0159\u00edv\u011bj\u0161\u00ed forma jm\u00e9na")
    HISTORICKÁ_DŘÍVĚJŠÍ_FORMA_JMÉNA("historick\u00e1 / d\u0159\u00edv\u011bj\u0161\u00ed forma jm\u00e9na"),
    @XmlEnumValue("jedin\u00fd zn\u00e1m\u00fd tvar jm\u00e9na v dan\u00e9m obdob\u00ed")
    JEDINÝ_ZNÁMÝ_TVAR_JMÉNA_V_DANÉM_OBDOBÍ("jedin\u00fd zn\u00e1m\u00fd tvar jm\u00e9na v dan\u00e9m obdob\u00ed"),
    @XmlEnumValue("um\u011ble vytvo\u0159en\u00e9 ozna\u010den\u00ed")
    UMĚLE_VYTVOŘENÉ_OZNAČENÍ("um\u011ble vytvo\u0159en\u00e9 ozna\u010den\u00ed"),
    @XmlEnumValue("singul\u00e1r")
    SINGULÁR("singul\u00e1r"),
    @XmlEnumValue("plur\u00e1l")
    PLURÁL("plur\u00e1l"),
    @XmlEnumValue("p\u0159ejat\u00fd term\u00edn")
    PŘEJATÝ_TERMÍN("p\u0159ejat\u00fd term\u00edn"),
    @XmlEnumValue("zastaral\u00fd, historick\u00fd term\u00edn")
    ZASTARALÝ_HISTORICKÝ_TERMÍN("zastaral\u00fd, historick\u00fd term\u00edn"),
    @XmlEnumValue("nevhodn\u00fd term\u00edn")
    NEVHODNÝ_TERMÍN("nevhodn\u00fd term\u00edn"),
    @XmlEnumValue("u\u017e\u0161\u00ed term\u00edn")
    UŽŠÍ_TERMÍN("u\u017e\u0161\u00ed term\u00edn"),
    @XmlEnumValue("odborn\u00fd term\u00edn")
    ODBORNÝ_TERMÍN("odborn\u00fd term\u00edn"),
    @XmlEnumValue("invertovan\u00e1 podoba")
    INVERTOVANÁ_PODOBA("invertovan\u00e1 podoba"),
    @XmlEnumValue("antonymum")
    ANTONYMUM("antonymum"),
    @XmlEnumValue("homonymum")
    HOMONYMUM("homonymum");
    private final String value;

    OznaceniTypTypA(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OznaceniTypTypA fromValue(String v) {
        for (OznaceniTypTypA c: OznaceniTypTypA.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
