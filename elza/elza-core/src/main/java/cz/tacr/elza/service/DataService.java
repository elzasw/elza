package cz.tacr.elza.service;

import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.vo.DataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class DataService {

    @Autowired
    private DataRepository dataRepository;

    public <ENTITY> List<ENTITY> findItemsWithData(Supplier<List<ENTITY>> getItems, Function<List<ENTITY>, List<DataResult>> getDataIds) {
        List<ENTITY> result = getItems.get();
        List<DataResult> dataIds = getDataIds.apply(result);
        dataRepository.findAllDataByDataResults(dataIds);
        return result;
    }

    public <ENTITY> ENTITY findItemWithData(Supplier<ENTITY> getItems, Function<List<ENTITY>, List<DataResult>> getDataIds) {
        ENTITY result = getItems.get();
        List<DataResult> dataIds = getDataIds.apply(Collections.singletonList(result));
        dataRepository.findAllDataByDataResults(dataIds);
        return result;
    }
}
