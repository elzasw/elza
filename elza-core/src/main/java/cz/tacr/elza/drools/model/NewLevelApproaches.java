package cz.tacr.elza.drools.model;

import java.util.LinkedList;
import java.util.List;


/**
 * Zastøešující tøída pro scénáøe
 *
 * @author Martin Šlapa
 * @since 23.12.2015
 */
public class NewLevelApproaches {

    /**
     * seznam scénáøù
     */
    List<NewLevelApproach> newLevelApproaches = new LinkedList<>();

    public NewLevelApproach create(final String name) {
        NewLevelApproach newLevelApproach = new NewLevelApproach(name);
        newLevelApproaches.add(newLevelApproach);
        return newLevelApproach;
    }

    public List<NewLevelApproach> getNewLevelApproaches() {
        return newLevelApproaches;
    }
}
