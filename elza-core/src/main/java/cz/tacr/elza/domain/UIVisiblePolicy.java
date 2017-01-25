package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Určení typů kontrol, validací, archivního popisu, které budou viditelné z UI.
 * Validace je vždy u archivního popisu uložena celá, všechny chyby,
 * ale na UI mohu potlačit zobrazení vybraných typů chyb validace.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@Entity(name = "ui_visible_policy")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UIVisiblePolicy {

    @Id
    @GeneratedValue
    private Integer visiblePolicyId;

    @Column(nullable = false)
    private Boolean visible;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policyTypeId", nullable = false)
    private RulPolicyType policyType;

    /**
     * @return identifikátor položky
     */
    public Integer getVisiblePolicyId() {
        return visiblePolicyId;
    }

    /**
     * @param visiblePolicyId  identifikátor položky
     */
    public void setVisiblePolicyId(final Integer visiblePolicyId) {
        this.visiblePolicyId = visiblePolicyId;
    }

    /**
     * Příznak, zda mají být chyby daného typu validace v UI zobrazeny nebo ne. Příznak platí pro celý podstrom
     * archivního popisu až do úrovně, kde je pro daný typ validace uvedena hodnota jiná. Když příznak uveden není,
     * daný typ validace je zobrazen.
     *
     * @return viditelný
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * @param visible viditelný
     */
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    /**
     * @return uzel
     */
    public ArrNode getNode() {
        return node;
    }

    /**
     * @param node uzel
     */
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    /**
     * @return typ kontrol
     */
    public RulPolicyType getPolicyType() {
        return policyType;
    }

    /**
     * @param policyType typ kontrol
     */
    public void setPolicyType(final RulPolicyType policyType) {
        this.policyType = policyType;
    }
}
