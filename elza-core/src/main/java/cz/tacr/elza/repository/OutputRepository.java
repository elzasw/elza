package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro verzi pojmenovaného výstupu z archivního výstupu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface OutputRepository extends JpaRepository<ArrOutput, Integer> {

}
