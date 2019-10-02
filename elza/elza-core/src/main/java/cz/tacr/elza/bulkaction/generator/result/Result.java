package cz.tacr.elza.bulkaction.generator.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Výsledek hromadné akce.
 *
 * @author Martin Šlapa
 * @since 30.06.2016
 */
public class Result {

    private List<ActionResult> results = new ArrayList<>();

    /**
     * @return seznam výsledků akcí
     */
    public List<ActionResult> getResults() {
        return results;
    }

    /**
     * @param results seznam výsledků akcí
     */
    public void setResults(final List<ActionResult> results) {
        this.results = results;
    }
}
