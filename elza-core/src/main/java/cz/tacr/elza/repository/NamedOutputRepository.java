package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNamedOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Respozitory pro výstup z archivního souboru.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface NamedOutputRepository extends JpaRepository<ArrNamedOutput, Integer> {

}
