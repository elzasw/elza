package cz.tacr.elza.api;

/**
 * Určení typů kontrol, validací, archivního popisu, které budou viditelné z UI.
 * Validace je vždy u archivního popisu uložena celá, všechny chyby,
 * ale na UI mohu potlačit zobrazení vybraných typů chyb validace.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
public interface UIVisiblePolicy<N extends ArrNode, PT extends RulPolicyType> {

    /**
     * @return identifikátor položky
     */
    Integer getVisiblePolicyId();

    /**
     * @param visiblePolicyId  identifikátor položky
     */
    void setVisiblePolicyId(Integer visiblePolicyId);

    /**
     * Příznak, zda mají být chyby daného typu validace v UI zobrazeny nebo ne. Příznak platí pro celý podstrom
     * archivního popisu až do úrovně, kde je pro daný typ validace uvedena hodnota jiná. Když příznak uveden není,
     * daný typ validace je zobrazen.
     *
     * @return viditelný
     */
    Boolean getVisible();

    /**
     * @param visible viditelný
     */
    void setVisible(Boolean visible);

    /**
     * @return uzel
     */
    ArrNode getNode();

    /**
     * @param node uzel
     */
    void setNode(N node);

    /**
     * @return typ kontrol
     */
    PT getPolicyType();

    /**
     * @param policyType typ kontrol
     */
    void setPolicyType(PT policyType);
}
