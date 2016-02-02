package cz.tacr.elza.repository;

import java.util.Set;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
public interface RegisterTypeRepositoryCustom {

    /**
     * Najde všechny potomky až k listům pro daný typ. Id typu je součástí výsledné množiny.
     *
     * @param registerTypeId typ, pro který hledáme potomky
     * @return množina id potomků
     */
    Set<Integer> findSubtreeIds(Integer registerTypeId);
}
