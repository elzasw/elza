package cz.tacr.elza.controller.factory;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrNodeConformityErrors;
import cz.tacr.elza.domain.ArrNodeConformityInfo;
import cz.tacr.elza.domain.ArrNodeConformityInfoExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.repository.NodeConformityErrorsRepository;
import cz.tacr.elza.repository.NodeConformityMissingRepository;


/**
 * Tovární třída pro vytváření rozšířených doménových objektů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 26.11.2015
 */
@Component
public class ExtendedObjectsFactory {

    @Autowired
    private NodeConformityMissingRepository nodeConformityMissingRepository;

    @Autowired
    private NodeConformityErrorsRepository nodeConformityErrorsRepository;


    /**
     * Vytvoří rozšířený objekt {@link cz.tacr.elza.api.ArrNodeConformityMissing}.
     *
     * @param conformityInfo chyba
     * @param loadErrors     true pokud se mají načítat chybějící a špatně zadaný hodnoty
     * @return rozšířený objekt chyby
     */
    public ArrNodeConformityInfoExt createNodeConformityInfoExt(@Nullable final ArrNodeConformityInfo conformityInfo,
                                                                final boolean loadErrors) {
        if (conformityInfo == null) {
            return null;
        }

        ArrNodeConformityInfoExt result = new ArrNodeConformityInfoExt();
        BeanUtils.copyProperties(conformityInfo, result);
        if (loadErrors) {
            List<ArrNodeConformityMissing> missing = nodeConformityMissingRepository
                    .findByNodeConformityInfo(conformityInfo);
            List<ArrNodeConformityErrors> errors = nodeConformityErrorsRepository
                    .findByNodeConformityInfo(conformityInfo);

            result.setMissingList(missing);
            result.setErrorList(errors);
        }

        return result;
    }


}
