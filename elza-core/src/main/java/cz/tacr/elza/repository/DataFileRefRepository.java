package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataFileRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * ArrDataFileRef repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface DataFileRefRepository extends JpaRepository<ArrDataFileRef, Integer> {

}
