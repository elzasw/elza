package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitalRepository;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 05.12.2016
 */
@Repository
public interface DigitalRepositoryRepository extends ElzaJpaRepository<ArrDigitalRepository, Integer> {

    ArrDigitalRepository findOneByCode(String code);

}
