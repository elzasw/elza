package cz.tacr.elza.controller.vo.filter;

import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Podmínky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 4. 2016
 */
public enum Condition {
    NONE("UNITDATE", "INT", "DECIMAL", "TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "PARTY_REF", "RECORD_REF", "COORDINATES", "DATE"),

    EMPTY("UNITDATE", "INT", "DECIMAL", "TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "PARTY_REF", "RECORD_REF", "COORDINATES", "DATE"),

    NOT_EMPTY("UNITDATE", "INT", "DECIMAL", "TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "PARTY_REF", "RECORD_REF", "COORDINATES", "DATE"),

    UNDEFINED("UNITDATE", "INT", "DECIMAL", "TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "PARTY_REF", "RECORD_REF", "COORDINATES", "DATE"),

    GT("UNITDATE", "INT", "DECIMAL", "DATE"),

    GE("INT", "DECIMAL", "DATE"),

    LT("UNITDATE", "INT", "DECIMAL", "DATE"),

    LE("INT", "DECIMAL", "DATE"),

    EQ("UNITDATE", "INT", "DECIMAL", "TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "DATE"),

    NE("INT", "DECIMAL", "DATE"),

    INTERVAL("INT", "DECIMAL", "DATE"),

    NOT_INTERVAL("INT", "DECIMAL", "DATE"),

    CONTAIN("TEXT", "STRING", "FORMATTED_TEXT", "UNITID", "PARTY_REF", "RECORD_REF"),

    NOT_CONTAIN("TEXT", "STRING", "FORMATTED_TEXT", "UNITID"),

    BEGIN("TEXT", "STRING", "FORMATTED_TEXT", "UNITID"),

    END("TEXT", "STRING", "FORMATTED_TEXT", "UNITID"),

    SUBSET("UNITDATE"),

    INTERSECT("UNITDATE");

    private List<String> supportedDescItemTypes;

    Condition(final String... supportedTypes) {
        Assert.notEmpty(supportedTypes);

        supportedDescItemTypes = Arrays.asList(supportedTypes);
    }

    /**
     * Zjistí zda podmínka podpruje daný typ atributu. Pokud ne tak vyhodí výjimku.
     *
     * @param typeCode kód typ atributu
     *
     * @throws IllegalStateException atribut není podporován
     */
    public void checkSupport(final String typeCode) {
        Assert.notNull(typeCode);

        if (!supportedDescItemTypes.contains(typeCode)) {
            throw new IllegalStateException("Tato podmínka nepodporuje atribut typu " + typeCode);
        }
    }
}
