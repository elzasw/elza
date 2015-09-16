package cz.tacr.elza.api;

/**
 * TODO: dospat komentář
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemText<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode> extends ArrDescItem<FC, RT, RS, N> {

    String getValue();


    void setValue(String value);
}
