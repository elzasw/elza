package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.AipType;
import cz.tacr.elza.controller.vo.LinkType;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.da.DaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AipController implements AipsApi {

    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaService daService;

    @Override
    public ResponseEntity<Void> aipCreateDaoStructure(List<Integer> aipIds) {
        daService.createDaoStructure(aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipImportPackage(Integer digitalRepositoryId, AipType aipType, Resource file) {
        try {
            try (InputStream is = file.getInputStream()) {
                ArrDigitalRepository digitalRepository = externalSystemService.getDigitalRepository(digitalRepositoryId);
                daService.processPackageInfo(digitalRepository, is, cz.tacr.elza.api.AipType.valueOf(aipType.name()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipCreateDaoLink(Integer aipId, Integer daoId, Integer nodeId, LinkType linkType) {
        daService.createDaoLink(aipId, daoId, nodeId, ArrDaoLink.LinkType.valueOf(linkType.name()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipDeleteDaoLink(Integer daoLinkId) {
        daService.deleteDaoLink(daoLinkId);
        return ResponseEntity.ok().build();
    }
}
