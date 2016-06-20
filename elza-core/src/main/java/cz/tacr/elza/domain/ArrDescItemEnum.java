package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemEnum extends ArrDescItem
        implements cz.tacr.elza.api.ArrDescItemEnum<ArrNode> {

    @Override
    public String toString() {
        return getItemSpec().getName();
    }
}
