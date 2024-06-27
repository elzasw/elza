package cz.tacr.elza.service;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.AipFilterVO;
import cz.tacr.elza.controller.vo.DaAipDetailVO;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.FilteredResult;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static cz.tacr.elza.exception.codes.ArrangementCode.AIP_NOT_FOUND;

@Service
public class AipService {

    @Autowired
    private AipRepository aipRepository;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    public FilteredResult<DaAip> findAipsByFilter(AipFilterVO[] filters, Integer from, Integer count) {
        return aipRepository.findAipsByFilter(filters, from, count);
    }

    public DaAipDetailVO getAip(@NotNull Integer id) {
        DaAip aip = aipRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Nenalezeno AIP s id " + id, AIP_NOT_FOUND));
        if(aip == null) {
            return null;
        }
        return clientFactoryVO.createAip(aip);
    }
}
