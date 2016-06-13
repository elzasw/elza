package cz.tacr.elza.api;

/**
 * Vazební tabulka mezi pravidlem a typem atribut - určení atributů, které jsou implicitní pro zobrazení, využívá se pro klilenta.
 *
 * @author Pavel Stánek
 * @since 10.06.2016
 */
public interface RulDefaultItemType<RS extends RulRuleSet, DIT extends RulItemType> {
    void setRuleSet(RS ruleSet);

    RS getRuleSet();

    void setItemType(DIT itemType);

    DIT getItemType();
}
