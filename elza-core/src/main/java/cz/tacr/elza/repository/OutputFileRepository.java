package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * OutputFile repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface OutputFileRepository extends JpaRepository<ArrOutputFile, Integer> {

}
