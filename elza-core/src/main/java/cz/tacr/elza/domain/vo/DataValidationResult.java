package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPolicyType;


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

    /**
     * Kód typu kontroly
     */
    private String policyTypeCode;

    /**
     * Typ kontroly
     */
    private RulPolicyType policyType;

    public DataValidationResult(final ValidationResultType resultType) {
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

    public void setPolicyTypeCode(final String policyTypeCode) {
        this.policyTypeCode = policyTypeCode;
    }

    public String getPolicyTypeCode() {
        return policyTypeCode;
    }

    public RulPolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(final RulPolicyType policyType) {
        this.policyType = policyType;
    }

    public enum ValidationResultType {
        MISSING,
        ERROR;
    }


}
