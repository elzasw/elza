package cz.tacr.elza.controller;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Třída pro validaci vstupních VO objektů (zkontroluje, že objekty mají vyplněny povinné hodnoty a že mají nastaveny
 * číselníky, které existují.)
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.01.2016
 */
@Service
public class ValidationVOService {

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;
    @Autowired
    private ApStateRepository apStateRepository;
    @Autowired
    private ApTypeRepository apTypeRepository;

    public ApState checkAccessPoint(Integer accessPointId) {
        if (accessPointId != null) {
            ApAccessPoint accessPoint = apAccessPointRepository.findById(accessPointId).orElse(null);
            if (accessPoint != null) {
                ApState state = apStateRepository.findLastByAccessPoint(accessPoint);
                if (state.getDeleteChange() != null) {
                    throw new IllegalStateException("Zneplatněné osodby není možno upravovat");
                }
                return state;
            }
        }
        return null;
    }
}
