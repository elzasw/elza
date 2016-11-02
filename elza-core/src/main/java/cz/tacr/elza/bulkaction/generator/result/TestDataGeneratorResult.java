package cz.tacr.elza.bulkaction.generator.result;

/**
 * Výsledek z hromadné akce {@link cz.tacr.elza.bulkaction.generator.TestDataGenerator}
 *
 * @author Petr Pytelka
 * @since 25.10.2016
 */
public class TestDataGeneratorResult extends ActionResult {

    private Integer countChanges;

    public Integer getCountChanges() {
        return countChanges;
    }

    public void setCountChanges(final Integer countChanges) {
        this.countChanges = countChanges;
    }

}
