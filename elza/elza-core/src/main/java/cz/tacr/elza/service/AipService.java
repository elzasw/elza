package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrAip;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.FilteredResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static cz.tacr.elza.exception.codes.ArrangementCode.AIP_NOT_FOUND;

@Service
public class AipService {

    @Autowired
    private AipRepository aipRepository;

    public FilteredResult<ArrAip> findAips(String search, Integer from, Integer count) {
        return aipRepository.findAips(search, from, count);
    }

    public ArrAip getAip(Integer aipId) {
        return aipRepository.findById(aipId).orElseThrow(() -> new ObjectNotFoundException("Nenalezeno AIP s id " + aipId, AIP_NOT_FOUND));
    }
}
