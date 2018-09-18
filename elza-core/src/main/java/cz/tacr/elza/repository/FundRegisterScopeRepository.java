package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFundRegisterScope;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link ArrFundRegisterScope}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 27.01.2016
 */
@Repository
public interface FundRegisterScopeRepository extends JpaRepository<ArrFundRegisterScope, Integer> {

    /**
     * Najde spojení na FA podle třídy.
     *
     * @param scope třída
     * @return seznam napojení
     */
    List<ArrFundRegisterScope> findByScope(ApScope scope);

    /**
     * Najde spojení na FA podle třídy.
     *
     * @param fund třída
     * @return seznam napojení
     */
    List<ArrFundRegisterScope> findByFund(ArrFund fund);

    ArrFundRegisterScope findByFundAndScope(ArrFund fund, ApScope scope);
}
