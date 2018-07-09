package cz.tacr.elza.service;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.service.vo.ApAccessPointData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccessPointDataService {

    @Autowired
    private ApNameRepository apNameRepository;

    @Autowired
    private ApDescriptionRepository apDescriptionRepository;

    @Autowired
    private ApExternalIdRepository apExternalIdRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    public ApAccessPointData findAccessPointData(ApAccessPoint apAccessPoint){
        ApDescription description = apDescriptionRepository.findByAccessPoint(apAccessPoint);
        List<ApName> names = apNameRepository.findByAccessPoint(apAccessPoint);
        List<ApExternalId> externalIds = apExternalIdRepository.findByAccessPoint(apAccessPoint);

        ApAccessPointData apData = new ApAccessPointData();
        apData.setAccessPoint(apAccessPoint);
        apData.setDescription(description);
        names.forEach(apData::addName);
        externalIds.forEach(apData::addExternalId);
        return apData;
    }

    public Map<Integer, ApAccessPointData> mapAccessPointDataById(Collection<Integer> apIds) {
        return apIds.stream().map(this::findAccessPointData).collect(Collectors.toMap(ApAccessPointData::getAccessPointId, Function.identity()));
    }

    public List<ApAccessPointData> findAccessPointData(Collection<ApAccessPoint> apRecords) {
        return apRecords.stream().map(this::findAccessPointData).collect(Collectors.toList());
    }

    public ApAccessPointData findAccessPointData(Integer accessPointId) {
        ApAccessPoint accessPoint = apAccessPointRepository.getOne(accessPointId);
        return findAccessPointData(accessPoint);
    }
}
