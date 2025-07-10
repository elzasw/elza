package cz.tacr.elza.drools.model;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.core.data.StaticDataProvider;


/**
 * Zastřešující třída pro scénáře
 *
 * @since 23.12.2015
 */
public class NewLevelApproaches {

    /**
     * seznam scénářů
     */
    List<NewLevelApproach> newLevelApproaches = new LinkedList<>();
    
	private final StaticDataProvider staticDataProvider;

    public NewLevelApproaches(StaticDataProvider sdp) {
		this.staticDataProvider = sdp;
	}

	public NewLevelApproach create(final String name) {
        NewLevelApproach newLevelApproach = new NewLevelApproach(name, staticDataProvider);
        newLevelApproaches.add(newLevelApproach);
        return newLevelApproach;
    }

    public List<NewLevelApproach> getNewLevelApproaches() {
        return newLevelApproaches;
    }
}
