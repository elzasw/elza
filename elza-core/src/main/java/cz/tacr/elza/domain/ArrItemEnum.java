package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemEnum extends ArrItemData implements cz.tacr.elza.api.ArrItemEnum {

    @Override
    public String toString() {
        return spec == null ? null : spec.getName();
    }
}
