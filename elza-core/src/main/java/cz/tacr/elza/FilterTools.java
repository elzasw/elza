package cz.tacr.elza;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.util.Assert;

import cz.tacr.elza.service.FilterTreeService;


/**
 * Pomocné metody pro filtrování dat.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @see FilterTreeService
 * @since 18.03.2016
 */
public class FilterTools {

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
        Assert.notNull(items);
        Assert.isTrue(page >= 0);
        Assert.isTrue(pageSize > 0);


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
        Assert.notNull(values);

        return values.removeIf(i -> i == null);
    }
}
