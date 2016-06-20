package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemUnitid<N extends ArrNode> extends ArrDescItem<N> {

    String getValue();


    void setValue(String value);
}
