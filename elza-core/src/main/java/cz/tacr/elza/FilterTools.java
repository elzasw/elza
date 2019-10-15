package cz.tacr.elza;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.service.FilterTreeService;


/**
 * Pomocné metody pro filtrování dat.
 *
 * @see FilterTreeService
 * @since 18.03.2016
 */
public class FilterTools {

    private FilterTools() {
        // Cannot be instantiated
    }

    /**
     * Vrací podmnožinu pole podle stránky a velikosti stránky.
     *
     * @param page     číslo stránky (od 0)
     * @param pageSize velikost stránky
     * @param items    záznamy pole
     * @param <T>      typ entity v poli
     * @return podpole nebo prázdné pole, pokud indexy přesahují rozsah pole
     */
    public static <T> ArrayList<T> getSublist(final int page, final int pageSize, final ArrayList<T> items) {
        Validate.notNull(items, "Položky nesmí být prázdné");
        Validate.isTrue(page >= 0, "Počet stran musí být kladné");
        Validate.isTrue(pageSize > 0, "Velikost stránky musí být větší než 0");


        int fromIndex = page * pageSize;
        if (fromIndex >= items.size()) {
            return new ArrayList<>();
        }

        int toIndex = (page + 1) * pageSize;

        toIndex = toIndex > items.size() ? items.size() : toIndex;
        return new ArrayList<>(items.subList(fromIndex, toIndex));
    }

    /**
     * Odebere ze vstupní seznamu null hodnoty.
     *
     * @param values zdrojový seznam
     *
     * @return příznak zda byl ve vstupních datech null
     */
    public static <T> boolean removeNullValues(final Collection<T> values) {
        Validate.notNull(values, "Hodnoty musí být vyplněny");

        return values.removeIf(Objects::isNull);
    }
}
