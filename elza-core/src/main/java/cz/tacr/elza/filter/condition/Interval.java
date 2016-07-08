package cz.tacr.elza.filter.condition;

import org.junit.Assert;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class Interval<IV> {

    private IV from;

    private IV to;

    /**
     * Alespoň jedna hodnota musí být předána.
     *
     * @param from začátek intervalu
     * @param to konec intervalu
     */
    public Interval(final IV from, final IV to) {
        Assert.assertTrue(from != null ||to != null);

        this.from = from;
        this.to = to;
    }

    public IV getFrom() {
        return from;
    }

    public IV getTo() {
        return to;
    }
}
