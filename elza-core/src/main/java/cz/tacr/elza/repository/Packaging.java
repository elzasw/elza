package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;

import java.util.List;

/**
 * Interface pro repozitory, u kterých má entita vazbu na RulPackage.
 *
 * @author Martin Šlapa
 * @since 22.11.2016
 */
public interface Packaging<T> {

    List<T> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

}
