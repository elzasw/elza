package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * Validační chyba při validaci atributu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 28.11.2015
 */
public class DataValidationResult {

    /**
     * Typ chyby.
     */
    private ValidationResultType resultType;
    /**
     * Popis chyby.
     */
    private String message;

    /**
     * Hodnota atributu.
     */
    private ArrDescItem descItem;

    /**
     * Id atributu, ke kterému se vztahuje chyba.
     */
    private Integer descItemId;

    /**
     * Typ atributu.
     */
    private RulDescItemType type;
    /**
     * Kód typu atributu, který chybí
     */
    private String typeCode;

    /**
     * Specifikace atributu.
     */
    private RulDescItemSpec spec;

    private DataValidationResult(final ValidationResultType resultType) {
        this.resultType = resultType;
    }


    public ValidationResultType getResultType() {
        return resultType;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public ArrDescItem getDescItem() {
        return descItem;
    }

    public void setDescItem(final ArrDescItem descItem) {
        this.descItem = descItem;
    }

    public RulDescItemType getType() {
        return type;
    }

    public void setType(final RulDescItemType type) {
        this.type = type;
    }

    public RulDescItemSpec getSpec() {
        return spec;
    }

    protected void setSpec(final RulDescItemSpec spec) {
        this.spec = spec;
    }

    public Integer getDescItemId() {
        return descItemId;
    }

    protected void setDescItemId(final Integer descItemId) {
        this.descItemId = descItemId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    protected void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    public enum ValidationResultType {
        MISSING,
        ERROR;
    }


    public static DataValidationResult createError(final ArrDescItem item, final String errorMsg) {
        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItem(item);
        result.setMessage(errorMsg);
        return result;
    }

    public static DataValidationResult createError(final Integer descItemId, final String errorMsg){
        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItemId(descItemId);
        result.setMessage(errorMsg);
        return result;
    }

    public static DataValidationResult createMissing(final RulDescItemType type,
                                                     final RulDescItemSpec spec) {
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setType(type);
        result.setSpec(spec);

        if (spec == null) {
            result.setMessage("Atribut " + type.getName() + " musí být vyplněn u této jednotky archivního popisu.");
        } else {
            result.setMessage("Atribut " + type.getName() + " se specifikací " + spec.getName()
                    + " musí být vyplněn u této jednotky archivního popisu.");
        }
        return result;
    }

    public static DataValidationResult createMissing(final String typeCode, final String message){
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setTypeCode(typeCode);
        result.setMessage(message);

        return result;
    }
}
