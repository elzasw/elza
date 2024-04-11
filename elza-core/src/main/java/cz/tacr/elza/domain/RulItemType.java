package cz.tacr.elza.domain;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Length;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.integer.DisplayType;
import cz.tacr.elza.domain.table.ElzaColumn;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;


/**
 *
 * Evidence typů atributů archivního popisu. evidence je společná pro všechny archivní pomůcky.
 *
 */
@Entity(name = "rul_item_type")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"}),
        @UniqueConstraint(columnNames = {"viewOrder"})})
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemType {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final TypeReference<List<ElzaColumn>> ELZA_COLUMNS = new TypeReference<List<ElzaColumn>>(){};

    public static final TypeReference<DisplayType> DISPLAY_TYPE = new TypeReference<DisplayType>(){};

    public static final String FIELD_VIEW_ORDER = "viewOrder";
    public static final String CODE = "code";

    @Id
    @GeneratedValue
    @Column(name = "item_type_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemTypeId;

    @Column(updatable = false, insertable = false)
    private Integer dataTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String shortcut;

    @Column(length = Length.LONG, nullable = false)
    private String description;

    @Column(nullable = false)
    private Boolean isValueUnique;

    @Column(nullable = false)
    private Boolean canBeOrdered;

    @Column(nullable = false)
    private Boolean useSpecification;

    @Column(nullable = false)
    private Integer viewOrder;

    @Column
    private String viewDefinition;

	// Note: Consider to remove all transient fields from this
	// class. Should be probably placed in RulItemTypeExt

    @Transient
    private Type type;

    @Transient
    private Boolean repeatable;

    @Transient
    private Boolean calculable;

    @Transient
    private Boolean calculableState;

    @Transient
    private String policyTypeCode;

    @Transient
    private Boolean indefinable;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructuredType.class)
    @JoinColumn(name = "structuredTypeId")
    private RulStructuredType structuredType;

    @Column(updatable = false, insertable = false)
    private Integer structuredTypeId;

    @Column()
    private Integer stringLengthLimit;

    /**
	 * Default constructor
	 */
	public RulItemType() {

	}

	/**
	 * Copy constructor for derived types
	 *
	 * @param src
	 */
	protected RulItemType(RulItemType src) {
		itemTypeId = src.getItemTypeId();
		dataTypeId = src.getDataTypeId();
		dataType = src.getDataType();
		code = src.getCode();
		name = src.getName();
		shortcut = src.getShortcut();
		description = src.getDescription();
		isValueUnique = src.getIsValueUnique();
		canBeOrdered = src.getCanBeOrdered();
		useSpecification = src.getUseSpecification();
		viewOrder = src.getViewOrder();
		setViewDefinition(src.getViewDefinition());
		type = src.getType();
		repeatable = src.getRepeatable();
		calculable = src.getCalculable();
		calculableState = src.getCalculableState();
		policyTypeCode = src.getPolicyTypeCode();
		indefinable = src.getIndefinable();
		rulPackage = src.getRulPackage();
		structuredType = src.getStructuredType();
		structuredTypeId = src.getStructuredTypeId();
        stringLengthLimit = src.getStringLengthLimit();
	}

	public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(final Integer descItemTypeId) {
        this.itemTypeId = descItemTypeId;
    }

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    public RulDataType getDataType() {
        return dataType;
    }

    public void setDataType(final RulDataType dataType) {
        this.dataType = dataType;
        this.dataTypeId = dataType != null ? dataType.getDataTypeId() : null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    /**
     * @return popis atributu, který slouží zároveň jako nápověda v aplikaci o jaký typ se jedná a jak se sním zachází.
     */
    public String getDescription() {
        return description;
    }

    /**
     * popis atributu, který slouží zároveň jako nápověda v aplikaci o jaký typ se jedná a jak se sním zachází.
     *
     * @param description popis atributu.
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return příznak, zda je hodnota atributu při použití tohoto typu jedinečná v rámci celé archivní pomůcky.
     */
    public Boolean getIsValueUnique() {
        return isValueUnique;
    }

    /**
     * příznak, zda je hodnota atributu při použití tohoto typu jedinečná v rámci celé archivní pomůcky.
     *
     * @param isValueUnique příznak.
     */
    public void setIsValueUnique(final Boolean isValueUnique) {
        this.isValueUnique = isValueUnique;
    }

    /**
     * @return příznak, zda je možné dle tohoto typu atributu setřídit archivní popis. zatím nebude aplikačně využíváno
     */
    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    /**
     * nastaví příznak, zda je možné dle tohoto typu atributu setřídit archivní popis.
     *
     * @param canBeOrdered příznak, zda je možné dle tohoto typu atributu setřídit archivní popis.
     */
    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    /**
     * @return příznak, zda se u typu atributu používají specifikace hodnot jako např. u druhů
     * jednotek popisu nebo u rolí entit. true = povinná specifikace, false = specifikace
     * neexistují. specifikace jsou uvedeny v číselníku rul_desc_item_spe.
     */
    public Boolean getUseSpecification() {
        return useSpecification;
    }

    /**
     * příznak, zda se u typu atributu používají specifikace hodnot jako např. u druhů jednotek
     * popisu nebo u rolí entit. true = povinná specifikace, false = specifikace neexistují.
     * specifikace jsou uvedeny v číselníku rul_desc_item_spe.
     *
     * @param useSpecification příznak, zda se u typu atributu používají specifikace hodnot.
     */
    public void setUseSpecification(final Boolean useSpecification) {
        this.useSpecification = useSpecification;
    }

    /**
     * @return pořadí typu atributu pro zobrazení v ui. pokud není pořadí uvedeno nebo je u více
     * typů uvedeno stejné pořadí, bude výsledné pořadí náhodné.
     */
    public Integer getViewOrder() {
        return viewOrder;
    }

    /**
     * nastaví pořadí typu atributu pro zobrazení v ui. pokud není pořadí uvedeno nebo je u více
     * typů uvedeno stejné pořadí, bude výsledné pořadí náhodné..
     *
     * @param viewOrder pořadí typu atributu pro zobrazení v ui.
     */
    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    /**
     * @return typ udává, zda je povinné/doporučené/... vyplnit hodnotu atributu.
     */
    public Type getType() {
        return type;
    }

    /**
     * Typ udává, zda je povinné/doporučené/... vyplnit hodnotu atributu.
     *
     * @param type typ
     */
    public void setType(final Type type) {
        this.type = type;
    }

    /**
     * Nastaví druh povinnosti vyplnění na minimálně danou hladinu
     *
     * @param type
     */
    public void setMinType(final Type type) {
        if (this.type != null) {
            switch (type) {
            case IMPOSSIBLE:
                if (this.type != Type.IMPOSSIBLE) {
                    // nothing to change
                    return;
                }
                break;
            case POSSIBLE:
                if (this.type == Type.RECOMMENDED || this.type == Type.REQUIRED) {
                    // nothing to change
                    return;
                }
                break;
            case RECOMMENDED:
                if (this.type == Type.REQUIRED) {
                    // nothing to change
                    return;
                }
                break;
            case REQUIRED:
                break;
            }
        }
        this.type = type;
    }

    /**
     * Nastaví druh povinnosti vyplnění na maximálně danou hladinu
     * 
     * @param type
     */
    public void setMaxType(final Type type) {
        if (this.type != null) {
            switch (type) {
            case REQUIRED:
                if (this.type != Type.REQUIRED) {
                    // nothing to change
                    return;
                }
                break;
            case RECOMMENDED:
                if (this.type == Type.IMPOSSIBLE || this.type == Type.POSSIBLE) {
                    // nothing to change
                    return;
                }
                break;
            case POSSIBLE:
                if (this.type == Type.IMPOSSIBLE) {
                    // nothing to change
                    return;
                }
                break;
            case IMPOSSIBLE:
                break;
            }
        }
        this.type = type;
    }

    /**
     * @return příznak udává, zda je atribut opakovatelný
     */
    public Boolean getRepeatable() {
        return repeatable;
    }

    /**
     * Příznak udává, zda je atribut opakovatelný.
     *
     * @param repeatable opakovatelnost
     */
    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    /**
     * @return počítaný atribut
     */
    public Boolean getCalculable() {
        return calculable;
    }

    /**
     * @param calculable počítaný atribut
     */
    public void setCalculable(final Boolean calculable) {
        this.calculable = calculable;
    }

    /**
     * @return stav kalkulace?
     */
    public Boolean getCalculableState() {
        return calculableState;
    }

    /**
     * @param calculableState stav kalkulace?
     */
    public void setCalculableState(final Boolean calculableState) {
        this.calculableState = calculableState;
    }

    /**
     * @return lze typ atributu nastavit jako nedefinovaný?
     */
    public Boolean getIndefinable() {
        return indefinable;
    }

    /**
     * @param indefinable lze typ atributu nastavit jako nedefinovaný?
     */
    public void setIndefinable(final Boolean indefinable) {
        this.indefinable = indefinable;
    }

    /**
     * @return balíček
     */
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    /**
     * @return id strukturovaného typu
     */
    public Integer getStructuredTypeId() {
        return structuredTypeId;
    }

    /**
     * @return strukturovaný typ
     */
    public RulStructuredType getStructuredType() {
        return structuredType;
    }

    /**
     * @param structuredType strukturovaný typ
     */
    public void setStructuredType(final RulStructuredType structuredType) {
        this.structuredType = structuredType;
        this.structuredTypeId = structuredType != null ? structuredType.getStructuredTypeId() : null;
    }

    /**
     * @return typ kód typu kontroly
     */
    public String getPolicyTypeCode() {
        return policyTypeCode;
    }

    /**
     * @param policyTypeCode kód typu kontroly
     */
    public void setPolicyTypeCode(final String policyTypeCode) {
        this.policyTypeCode = policyTypeCode;
    }

    public Object getViewDefinition(TypeReference<?> typeReference) {
        if (viewDefinition == null) {
            return null;
        }
        try {
            return objectMapper.readValue(viewDefinition, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException("Problém při generování JSON", e);
        }
    }

    public Object getViewDefinition() {
        if (viewDefinition == null) {
            return null;
        }
        try {
            DataType dataType = DataType.fromId(getDataType().getDataTypeId());
            if (dataType == DataType.JSON_TABLE) {
                return objectMapper.readValue(viewDefinition, ELZA_COLUMNS);
            } else if (dataType == DataType.INT) {
                Object result = objectMapper.readValue(viewDefinition, DISPLAY_TYPE);
                return result == null ? DisplayType.NUMBER : result;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Problém při generování JSON", e);
        }
    }

    public void setViewDefinition(final Object viewDefinition) {
        if (viewDefinition == null) {
            return;
        }
        try {
            this.viewDefinition = objectMapper.writeValueAsString(viewDefinition);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
    }

    public Integer getStringLengthLimit() {
        return stringLengthLimit;
    }

    public void setStringLengthLimit(Integer stringLengthLimit) {
        this.stringLengthLimit = stringLengthLimit;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.RulItemType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.RulItemType other = (cz.tacr.elza.domain.RulItemType) obj;

        return new EqualsBuilder()
                .append(itemTypeId, other.getItemTypeId())
                .append(code, other.getCode())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(itemTypeId)
                .append(code)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "RulItemType pk=" + itemTypeId + ", code=" + code;
    }

    public enum Type {
        /**
         * Povinný
         */
        REQUIRED,

        /**
         * Doporučený
         */
        RECOMMENDED,

        /**
         * Možný
         */
        POSSIBLE,

        /**
         * Nemožný
         */
        IMPOSSIBLE
    }
}
