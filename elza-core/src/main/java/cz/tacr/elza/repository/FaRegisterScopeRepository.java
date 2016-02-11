package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFaRegisterScope;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RegScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


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

    /**
     * Najde spojení na FA podle třídy.
     *
     * @param findingAid třída
     * @return seznam napojení
     */
    List<ArrFaRegisterScope> findByFindingAid(ArrFindingAid findingAid);

    ArrFaRegisterScope findByFindingAidAndScope(ArrFindingAid findingAid, RegScope scope);
}
