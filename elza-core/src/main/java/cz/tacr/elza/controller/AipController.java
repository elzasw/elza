package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.service.AipService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.da.DaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class AipController implements AipsApi {
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private DaService daService;
    @Autowired
    private AipService aipService;
    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Override
    public ResponseEntity<Void> aipCreateDaoStructure(List<Integer> aipIds) {
        daService.createDaoStructure(aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipDeleteDaoStructure(List<Integer> aipIds) {
        daService.deleteDaoStructure(aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<AipDetailFilteredResult> aipFindByFilter(Integer from, Integer count, List<AipFilterGen> aipFilterGen) {
        FilteredResult<DaAip> aips = aipService.findAipDetailsByFilter(aipFilterGen, from, count);
        AipDetailFilteredResult result = new AipDetailFilteredResult();
        result.setCount(aips.getTotalCount());
        result.setRows(clientFactoryVO.createAips(aips.getList()));
        return ResponseEntity.ok(result);
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
    public ResponseEntity<TreeDataCustomGen> aipLevelViewTree(List<Integer> requestBody) {
        return ResponseEntity.ok(aipService.getAipsLogicalTree(requestBody));
    }

//    @Override
//    public ResponseEntity<TreeDataCustomGen> aipTree(Integer aipId) {
//        return ResponseEntity.ok(aipService.getAipTree(aipId));
//    }

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

    @Override
    public ResponseEntity<AipDetailVO> aipGetAip(Integer aipId) {
        return ResponseEntity.ok(aipService.getAipDetail(aipId));
    }


//    @RequestMapping(value = "/{aipId}", method = RequestMethod.GET)
//    public DaDaoFileFolderVO getDaDaoByAip(@PathVariable("aipId") final Integer aipId) {
//        return daoService.findByAipIdAndTypeAndDeleteChangeIsNull(aipId);
//    }
}
