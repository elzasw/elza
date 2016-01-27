package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFaRegisterScope;
import cz.tacr.elza.domain.RegScope;


/**
 * Repository pro {@link ArrFaRegisterScope}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Repository
public interface FaRegisterScopeRepository extends JpaRepository<ArrFaRegisterScope, Integer> {

    /**
     * Najde spojení na FA podle třídy.
     *
     * @param scope třída
     * @return seznam napojení
     */
    List<ArrFaRegisterScope> findByScope(RegScope scope);
}
