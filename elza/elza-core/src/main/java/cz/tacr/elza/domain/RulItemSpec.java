package cz.tacr.elza.domain;

import javax.persistence.*;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

import java.util.List;


/**
 * Evidence možných specifikací typů atributů archivního popisu. Evidence je společná pro všechny
 * archivní pomůcky. Vazba výčtu specifikací na různá pravidla bude řešeno později. Podtyp atributu
 * (Role entit - Malíř, Role entit - Sochař, Role entit - Spisovatel).
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_item_spec")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"})})
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulItemSpec {

    public static final String FIELD_VIEW_ORDER = "viewOrder";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemSpecId;

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

	@Column(length = StringLength.LENGTH_1000)
	private String category;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
	@JoinColumn(name = "packageId", nullable = false)
	private RulPackage rulPackage;

    @OneToMany(mappedBy = "itemSpec", fetch = FetchType.LAZY)
    private List<RulItemTypeSpecAssign> itemTypeSpecAssigns;

	// Consider to move transient fields to RulItemSpecExt
    @Transient
    private Type type;

    @Transient
    private Boolean repeatable;

    @Transient
    private String policyTypeCode;

    @Transient
    private Integer viewOrder;

	/**
	 * Default constructor
	 */
	public RulItemSpec() {

	}

	/**
	 * Copy constructor
	 *
	 * @param src
	 */
	public RulItemSpec(RulItemSpec src) {
		itemSpecId = src.itemSpecId;
		code = src.code;
		name = src.name;
		shortcut = src.shortcut;
		description = src.description;
		type = src.type;
		repeatable = src.repeatable;
		policyTypeCode = src.policyTypeCode;
		category = src.category;
		rulPackage = src.rulPackage;
	}

	public Integer getItemSpecId() {
        return itemSpecId;
    }

    public void setItemSpecId(final Integer itemSpecId) {
        this.itemSpecId = itemSpecId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
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

    /**
     * @return kategorie umožňující vytvoření stromu v UI ("Indoevropské|Slovanské")
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category kategorie umožňující vytvoření stromu v UI ("Indoevropské|Slovanské")
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /**
     * @return balíček
     */
    public RulPackage getPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    public List<RulItemTypeSpecAssign> getItemTypeSpecAssigns() {
        return itemTypeSpecAssigns;
    }

    public void setItemTypeSpecAssigns(final List<RulItemTypeSpecAssign> itemTypeSpecAssigns) {
        this.itemTypeSpecAssigns = itemTypeSpecAssigns;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.RulItemSpec)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.RulItemSpec other = (cz.tacr.elza.domain.RulItemSpec) obj;

        return new EqualsBuilder().append(itemSpecId, other.getItemSpecId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(itemSpecId).append(name).append(code).toHashCode();
    }

    @Override
    public String toString() {
        return "RulItemSpec pk=" + itemSpecId;
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
