package cz.tacr.elza.bulkaction.generator.result;

/**
 * Výsledek z hromadné akce {@link cz.tacr.elza.bulkaction.generator.GenerateUnitId}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class SerialNumberResult extends ActionResult {

    private Integer countChanges;

    public Integer getCountChanges() {
        return countChanges;
    }

    public void setCountChanges(final Integer countChanges) {
        this.countChanges = countChanges;
    }

}
