package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link ArrNodeExtension} - Custom.
 *
 * @since 23.10.2017
 */
@Repository
public interface NodeExtensionRepositoryCustom {

    /**
     * Vyhledá všechny aktivní vazby na rozšíření od root JP k JP předané parametrem.
     *
     * @param nodeId JP ke které vyhledávám od root JP
     * @return nalezené aktivní vazby
     */
    List<ArrNodeExtension> findAllByNodeIdFromRoot(Integer nodeId);

}
