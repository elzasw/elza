package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemFormattedText<FC extends ArrChange, RT extends RulItemType, RS extends RulItemSpec, N extends ArrNode> extends ArrDescItem<FC, RT, RS, N> {

    String getValue();


    void setValue(String value);
}
