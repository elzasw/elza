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
import cz.tacr.elza.common.io.SpooledContent;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.HttpStatus;

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
    public ResponseEntity<DaAipActionVO> aipCreateDaoStructure(List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.requestMetadata(aipIds)));
    }

    @Override
    public ResponseEntity<DaAipActionVO> aipDeleteDaoStructure(List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.deleteMetadata(aipIds)));
    }

    @Override
    public ResponseEntity<DaAipActionVO> aipDownloadCompleteAip(List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.aipDownloadCompleteAip(aipIds)));
    }

    @Override
    public ResponseEntity<Resource> aipDownloadComponent(Integer fileId) {
        return daService.getComponent(fileId);
    }

    @Override
    public ResponseEntity<List<AipPackageEntry>> aipListPackageEntries(Integer aipId) {
        return ResponseEntity.ok(daService.getPackageEntries(aipId));
    }

    @Override
    public ResponseEntity<Resource> aipDownloadPackage(Integer aipId) {
        return daService.getPackage(aipId);
    }

    @Override
    public ResponseEntity<Resource> aipDownloadPackageEntry(Integer aipId, String path) {
        SpooledContent content = daService.getPackageEntry(aipId, path);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_LENGTH, Long.toString(content.size()));
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + path.substring(path.lastIndexOf('/') + 1) + "\"");
            return new ResponseEntity<>(new InputStreamResource(content.openStreamAndCloseOnEnd()), headers,
                                        HttpStatus.OK);
        } catch (IOException e) {
            content.close();
            throw new SystemException("Nepodařilo se odeslat soubor balíčku", e, BaseCode.INVALID_STATE);
        }
    }

    @Override
    public ResponseEntity<DaAipActionVO> aipDeleteCompleteAip(List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.aipDeleteCompleteAip(aipIds)));
    }

    @Override
    public ResponseEntity<DaAipActionVO> aipUpdateAip(AipUpdateType type, List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.aipUpdateAip(type, aipIds)));
    }

    @Override
    public ResponseEntity<DaAipActionVO> aipExportAip(List<Integer> aipIds) {
        return ResponseEntity.ok(clientFactoryVO.createAipAction(daService.aipExportAip(aipIds)));
    }

    @Override
    public ResponseEntity<AipDetailFilteredResult> aipFindByFilter(SearchParams searchParams) {
        FilteredResult<DaAip> aips = aipService.findAipDetailsByFilter(searchParams);
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
