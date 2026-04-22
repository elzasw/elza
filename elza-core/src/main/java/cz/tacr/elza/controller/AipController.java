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
    public ResponseEntity<Void> aipDownloadCompleteAip(List<Integer> aipIds) {
        daService.aipDownloadCompleteAip(aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> aipDownloadComponent(Integer fileId) {
        return daService.getComponent(fileId);
    }

    @Override
    public ResponseEntity<Void> aipDeleteCompleteAip(List<Integer> aipIds) {
        daService.aipDeleteCompleteAip(aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipUpdateAip(AipUpdateType type, List<Integer> aipIds) {
        daService.aipUpdateAip(type, aipIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipExportAip(List<Integer> aipIds) {
        daService.aipExportAip(aipIds);
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
                daService.processPackageInfo(digitalRepository, is, cz.tacr.elza.api.AipType.valueOf(aipType.name()), new ArrayList<>());
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

    @Override
    public ResponseEntity<DaoLinksResult> aipGetDaoLinks(Integer nodeId) {
        return ResponseEntity.ok(daService.getDaoLinks(nodeId));
    }

    @Override
    public ResponseEntity<Void> aipBulkConnectLogicToJp(Integer arrNodeId, List<Integer> daAipId, Integer daDaoId) {
        daService.bulkConnectLogicalStructureToJP(arrNodeId, daAipId, daDaoId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipBulkConnectToJp(Integer arrNodeId, List<Integer> daAipIdList) {
        daService.bulkConnectToJP(arrNodeId, daAipIdList);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipBulkCreateFromSelected(Integer arrNodeId, List<Integer> daAipIdList) {
        daService.bulkCreateFromSelected(arrNodeId, daAipIdList);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> aipBulkCreateSelectedToJp(Integer arrNodeId, List<Integer> daAipIdList, Integer daLevelViewId) {
        daService.bulkCreateFromSelectedToJP(arrNodeId, daAipIdList, daLevelViewId);
        return ResponseEntity.ok().build();
    }

    /**
     * Vytvořit JP s propojením
     * @param arrNodeId ArrNode ID (required)
     * @param daAipId DaAip ID (required)
     * @param daDaoIdList DaDao ID List (required)
     * @return
     */
    @Override
    public ResponseEntity<Void> aipConnectJpFromSelected(Integer arrNodeId, Integer daAipId, List<Integer> daDaoIdList) {
        daService.createJPFromSelectedList(arrNodeId, daAipId, daDaoIdList);
        return ResponseEntity.ok().build();
    }

    /**
     * Vytvořit JP z vybraných
     * @param arrNodeId ArrNode ID (required)
     * @param daAipId DaAip ID (required)
     * @param daDaoIdList DaDao ID List (required)
     * @return
     */
    @Override
    public ResponseEntity<Void> aipConnectJpLinkFromSelected(Integer arrNodeId, Integer daAipId, List<Integer> daDaoIdList) {
        daService.createAndLinkFromSelectedList(arrNodeId, daAipId, daDaoIdList);
        return ResponseEntity.ok().build();
    }

    /**
     * Připojit k JP - pokud jsem v části balíčku
     * @param arrNodeId ArrNode ID (required)
     * @param daAipId DaAip ID (required)
     * @param daDaoIdList DaDao ID List (required)
     * @return
     */
    @Override
    public ResponseEntity<Void> aipConnectPartToJp(Integer arrNodeId, Integer daAipId, List<Integer> daDaoIdList) {
        daService.connectPartListToJP(arrNodeId, daAipId, daDaoIdList);
        return ResponseEntity.ok().build();
    }


    /**
     * Výběrové připojení s JP
     * @param arrNodeId ArrNode ID (required)
     * @param daAipId DaAip ID (required)
     * @param daDaoIdList DaDao ID List (required)
     * @return
     */
    @Override
    public ResponseEntity<Void> aipConnectSelectedToJp(Integer arrNodeId, Integer daAipId, List<Integer> daDaoIdList) {
        daService.connectSelectedListToJP(arrNodeId, daAipId, daDaoIdList);
        return ResponseEntity.ok().build();
    }

    /**
     * Připojit k JP - pokud jsem na nejvyšší úrovni balíčku
     * @param arrNodeId ArrNode ID (required)
     * @param daAipId DaAip ID (required)
     * @return
     */
    @Override
    public ResponseEntity<Void> aipConnectToJp(Integer arrNodeId, Integer daAipId) {
        daService.connectToJP(arrNodeId, daAipId);
        return ResponseEntity.ok().build();
    }
}
