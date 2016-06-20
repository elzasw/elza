package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * DmsFile repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Repository
public interface FileRepository extends JpaRepository<DmsFile, Integer>, FileRepositoryCustom {
}
