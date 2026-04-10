package cz.tacr.elza.drools.service;

import cz.tacr.elza.drools.model.Ap;

/**
 * Provider for Ap
 * 
 * Provider is caching Ap by accessPointId
 */
public interface ApProvider {
	
	/**
	 * Return Ap by accessPointId
	 * @param accessPointId
	 * @return
	 */
	Ap getAp(Integer accessPointId);
}
