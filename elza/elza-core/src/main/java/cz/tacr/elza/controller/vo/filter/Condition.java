package cz.tacr.elza.controller.vo.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;

/**
 * Podmínky.
 *
 * @since 18. 4. 2016
 */
public enum Condition {
    NONE(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, 
    		DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID, 
    		DataType.RECORD_REF, DataType.COORDINATES, DataType.DATE),

    EMPTY(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, 
    		DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID,
    		DataType.RECORD_REF, DataType.COORDINATES, DataType.DATE),

    NOT_EMPTY(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, 
    		DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID,
    		DataType.RECORD_REF, DataType.COORDINATES, DataType.DATE),

    UNDEFINED(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, 
    		DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID,
    		DataType.RECORD_REF, DataType.COORDINATES, DataType.DATE),

    GT(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, DataType.DATE),

    GE(DataType.INT, DataType.DECIMAL, DataType.DATE),

    LT(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, DataType.DATE),

    LE(DataType.INT, DataType.DECIMAL, DataType.DATE),

    EQ(DataType.UNITDATE, DataType.INT, DataType.DECIMAL, 
    		DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID, 
    		DataType.DATE),

    NE(DataType.INT, DataType.DECIMAL, DataType.DATE),

    INTERVAL(DataType.INT, DataType.DECIMAL, DataType.DATE),

    NOT_INTERVAL(DataType.INT, DataType.DECIMAL, DataType.DATE),

    CONTAIN(DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID,
    		DataType.RECORD_REF),

    NOT_CONTAIN(DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID),

    BEGIN(DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID),

    END(DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT, DataType.UNITID),

    SUBSET(DataType.UNITDATE),

    INTERSECT(DataType.UNITDATE);

    private List<DataType> supportedDescItemTypes;

    Condition(final DataType... supportedTypes) {
        Validate.notEmpty(supportedTypes);

        supportedDescItemTypes = Arrays.asList(supportedTypes);
    }

    /**
     * Zjistí zda podmínka podpruje daný typ atributu. Pokud ne tak vyhodí výjimku.
     *
     * @param typeCode kód typ atributu
     *
     * @throws IllegalStateException atribut není podporován
     */
    public void checkSupport(final DataType dataType) {
        Objects.requireNonNull(dataType);

        if (!supportedDescItemTypes.contains(dataType)) {
            throw new IllegalStateException("Tato podmínka nepodporuje atribut typu " + dataType);
        }
    }
}
