package cz.tacr.elza.domain;

import java.io.IOException;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.table.ElzaColumn;


/**
 *
 * Evidence typů atributů archivního popisu. evidence je společná pro všechny archivní pomůcky.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_item_type")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"}),
        @UniqueConstraint(columnNames = {"viewOrder"})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemType {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(RulItemType.class);

    @Id
    @GeneratedValue
    @Column(name = "item_type_id")
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

    @Column(nullable = false)
    @Lob
    @org.hibernate.annotations.Type(type = "org.hibernate.type.TextType")
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
    private String columnsDefinition;

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

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
     * @return pravidla
     */
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * @param ruleSet pravidla
     */
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
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

    public List<ElzaColumn> getColumnsDefinition() {
        if (columnsDefinition == null) {
            return null;
        }
        try {
            return objectMapper.readValue(columnsDefinition, new TypeReference<List<ElzaColumn>>(){});
        } catch (IOException e) {
            throw new IllegalArgumentException("Problém při generování JSON", e);
        }
    }

    public void setColumnsDefinition(final List<ElzaColumn> columns) {
        try {
            columnsDefinition = objectMapper.writeValueAsString(columns);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
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
