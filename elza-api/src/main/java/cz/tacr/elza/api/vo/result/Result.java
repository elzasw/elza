package cz.tacr.elza.api.vo.result;

import java.io.Serializable;
import java.util.List;

/**
 * Výsledek hromadné akce.
 *
 * @author Martin Šlapa
 * @since 30.06.2016
 */
public interface Result<AR extends ActionResult> extends Serializable {

    /**
     * @return seznam výsledků akcí
     */
    List<AR> getResults();

    /**
     * @param results seznam výsledků akcí
     */
    void setResults(List<AR> results);
}
