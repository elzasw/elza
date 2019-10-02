package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemEnum extends ArrItemData {

    @Override
    public String toString() {
        return spec == null ? null : spec.getName();
    }
}
