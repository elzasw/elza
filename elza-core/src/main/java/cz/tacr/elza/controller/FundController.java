package cz.tacr.elza.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.CreateFund;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.controller.vo.FsRepo;
import cz.tacr.elza.controller.vo.Fund;
import cz.tacr.elza.controller.vo.FundDetail;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.UpdateFund;
import cz.tacr.elza.controller.vo.UsedItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.IOExportWorker;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ArrangementService.FindFundVersionsResult;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.dao.FileSystemRepoService;

@RestController
@RequestMapping("/api/v1")
public class FundController implements FundsApi {

    private static final Logger logger = LoggerFactory.getLogger(FundController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private IOExportWorker ioExportWorker;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private ClientFactoryVO factoryVo;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private StructObjService structureService;

    @Autowired
    private FileSystemRepoService fileSystemRepoService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private DaoService daoService;

    @Autowired
    private FundLevelService fundLevelService;

    // POST /fund
    @Override
    @Transactional
    public ResponseEntity<Fund> fundCreateFund(@RequestBody CreateFund createFund) {
        // Kontrola a vytvoření AS
        Validate.isTrue(StringUtils.isNotBlank(createFund.getName()), "Musí být vyplněn název");
        Validate.notNull(createFund.getInstitutionIdentifier(), "Identifikátor instituce musí být vyplněn");
        Validate.notNull(createFund.getRuleSetCode(), "Identifikátor pravidel musí být vyplněn");
        Validate.notNull(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");
        Validate.notEmpty(createFund.getScopes(), "Musí být zadána alespoň jedna oblast zařazení");

        StaticDataProvider sdp = staticDataService.getData();

        // prepare ruleset
        RuleSet ruleSet = sdp.getRuleSetByCode(createFund.getRuleSetCode());
        Validate.notNull(ruleSet, "Nebyla nalezena pravidla tvorby s kódem " + createFund.getRuleSetCode());

        // prepare institution
        ParInstitution institution = arrangementService.getInstitution(createFund.getInstitutionIdentifier());
        Validate.notNull(institution, "Nebyla nalezena instituce s identifikátorem " + createFund.getInstitutionIdentifier());

        // prepare collection of scopes
        List<ApScope> scopes = scopeRepository.findByCodes(createFund.getScopes());
        Validate.isTrue(scopes.size() == createFund.getScopes().size(),
                      "Některá oblast archivních entit nebyla nalezena");

        ArrFundVersion fundVersion = arrangementService.createFundWithScenario(
        								createFund.getName(), ruleSet.getEntity(), createFund.getInternalCode(),
                                        institution, createFund.getFundNumber(),
                                        createFund.getUnitdate(), createFund.getMark(),
                                        createFund.getUuid(), null,
                                        scopes, createFund.getAdminUsers(), createFund.getAdminGroups());

        ArrNode rootNode = fundVersion.getRootNode();

        return ResponseEntity.ok(factoryVo.createFund(fundVersion.getFund(), rootNode.getUuid()));
    }

//    @Deprecated
//    @Override
//    public ResponseEntity<FindFundsResult> fundFindFunds(@RequestParam(value = "fulltext", required = false) String fulltext,
//                                                     @RequestParam(value = "institutionIdentifier", required = false) String institutionIdentifier,
//                                                     @RequestParam(value = "max", required = false, defaultValue = "200") Integer max,
//                                                     @RequestParam(value = "from", required = false, defaultValue = "0") Integer from) {
//        //UserDetail userDetail = userService.getLoggedUserDetail();
//        //FilteredResult<ArrFund> funds;
//    	FieldValueFilter institutionIdFilter = null;
//        if (institutionIdentifier != null && !institutionIdentifier.isEmpty()) {
//            ParInstitution institution = arrangementService.getInstitution(institutionIdentifier);
//            if (institution != null) {
//                Integer institutionId = institution.getInstitutionId();
//                institutionIdFilter = new FieldValueFilter()
//                		                    .field(FondsFilterField.INSTITUTION_ID)
//                		                    .value(institutionId.toString())
//                							.operation(OperationCompareType.EQ);
//            } else {
//                return ResponseEntity.ok(new FindFundsResult());
//            }
//        }
//
//        FieldValueFilter nameFilter = new FieldValueFilter().field(FondsFilterField.NAME).value(fulltext)
//        		.operation(OperationCompareType.CONTAINS);
//
//        FieldValueFilter internalCodeFilter = new FieldValueFilter().field(FondsFilterField.INTERNAL_CODE).value(fulltext)
//        		.operation(OperationCompareType.CONTAINS);
//        
//        FieldValueFilter fundNumberFilter = new FieldValueFilter().field(FondsFilterField.FUND_NUMBER).value(fulltext)
//        		.operation(OperationCompareType.CONTAINS);
//
//        FieldValueFilter markFilter = new FieldValueFilter().field(FondsFilterField.MARK).value(fulltext)
//        		.operation(OperationCompareType.CONTAINS);
//
//        SearchParams searchParams = new SearchParams()
//        		.addFiltersItem(nameFilter)
//        		.addFiltersItem(internalCodeFilter)
//        		.addFiltersItem(fundNumberFilter)
//        		.addFiltersItem(markFilter)
//        		.offset(from)
//        		.size(max);
//
//        if (institutionIdFilter != null) {
//        	searchParams.addFiltersItem(institutionIdFilter);
//        }
//
//        FindFundVersionsResult fundVersionsResult = arrangementService.findFundsBySearchParams(searchParams);
//        List<Fund> funds = fundVersionsResult.getFundVersionList().stream().map(fv -> factoryVo.createFund(fv)).toList();
//
////        if (userDetail.hasPermission(UsrPermission.Permission.FUND_RD_ALL)) {
////            // read all funds
////            funds = fundRepository.findFunds(fulltext, institutionId, from, max);
////
////        } else {
////            Integer userId = userDetail.getId();
////            funds = fundRepository.findFundsWithPermissions(fulltext, institutionId, from, max, userId);
////        }
////
////        List<ArrFund> fundList = funds.getList();
////        FindFundsResult fundsResult = new FindFundsResult();
////        fundsResult.setTotalCount(funds.getTotalCount());
////        fundList.forEach(f -> {
////            Fund fund = factoryVo.createFund(f.getFund(), "TODO: uuid");
////            fundsResult.addFundsItem(fund);
////        });
//
////        return ResponseEntity.ok(fundsResult);
//        return ResponseEntity.ok(new FindFundsResult(funds, fundVersionsResult.getTotalCount()));
//    }

    // POST /fund/search
    @Override
    public ResponseEntity<FindFundsResult> fundSearchFunds(@RequestBody SearchParams searchParams) {
        FindFundVersionsResult fundVersionsResult = arrangementService.findFundsBySearchParams(searchParams);
        List<Fund> funds = fundVersionsResult.getFundVersionList().stream()
        		.map(fv -> factoryVo.createFund(fv))
        		.toList();

        return ResponseEntity.ok(new FindFundsResult(funds, fundVersionsResult.getTotalCount()));
    }

    // POST /fund/export
    @Override
    public ResponseEntity<Integer> fundExportFunds(@RequestBody SearchParams searchParams) {
        UsrUser user = userService.getLoggedUser();
        Integer userId = (user == null ? null : user.getUserId());
        String downloadFileName = "funds_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + ".csv";

        int id = ioExportWorker.addExportRequest(userId, downloadFileName, searchParams);
        return ResponseEntity.ok(id);
    }

    // GET /fund/{id}
    @Override
    public ResponseEntity<FundDetail> fundGetFund(@PathVariable("id") String id) {
        Validate.notNull(id, "Musí být zadáno id AS");

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(Integer.valueOf(id));
        if (fundVersion == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
        ArrNode rootNode = fundVersion.getRootNode();

        return ResponseEntity.ok(factoryVo.createFundDetail(fundVersion.getFund(), rootNode.getUuid()));
    }

    // PUT /fund/{id}/import
    @Override
    @Transactional
    public ResponseEntity<Void> fundImportFundData(@PathVariable("id") String id,
                                               	   @RequestPart(value = "importType", required = true) String importType,
                                               	   @RequestPart(value = "dataFile", required = true) MultipartFile dataFile) {
        Validate.notNull(id, "Musí být zadáno id AS");

        ArrFund fund = arrangementService.getFund(Integer.valueOf(id));
        try (InputStream is = dataFile.getInputStream()) {
            arrangementService.importFundData(fund, importType, is);
            return ResponseEntity.ok(null);
        } catch (AbstractException ae) {
        	logger.error("Failed to import data", ae);
        	throw ae;
        } catch (Exception e) {
        	// TODO: This should be probably removed - check general exception handler 
            logger.error("Failed to import data", e);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<FundDetail> fundUpdateFund(@PathVariable("id") String id, @RequestBody UpdateFund updateFund) {
        Validate.notNull(updateFund, "AS musí být vyplněn");
        Validate.notNull(updateFund.getRuleSetCode(), "AS musí mít přiřazená pravidla");

        ParInstitution institution = arrangementService.getInstitution(updateFund.getInstitutionIdentifier());

        List<ApScope> apScopes = FactoryUtils.transformList(updateFund.getScopes(), s -> accessPointService.getApScope(s));

        ArrFund arrFund = factoryDO.createFund(updateFund, institution, id);
        // TODO: use StaticData
        RulRuleSet ruleSet = ruleSetRepository.findByCode(updateFund.getRuleSetCode());
        Objects.requireNonNull(ruleSet);

        ArrFundVersion fundVersion = arrangementService.updateFund(arrFund, ruleSet, apScopes, null, null);
        ArrNode rootNode = fundVersion.getRootNode();

        return ResponseEntity.ok(factoryVo.createFundDetail(fundVersion.getFund(), rootNode.getUuid()));
    }

    /**
     * Smazání hodnot strukturovaného datového typu.
     *
     * @param fundVersionId    identifikátor verze AS
     * @param structureDataIds identifikátory hodnot strukturovaného datového typu
     * @return smazané entity
     */
    @Override
    @Transactional
    public ResponseEntity<List<Integer>> fundDeleteStructureData(final Integer fundVersionId, final List<Integer> structureDataIds) {
        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        List<ArrStructuredObject> structObjList = structureService.getStructObjByIds(structureDataIds);
        List<Integer> deletedIds = structureService.deleteStructObj(fundVersion.getFundId(), structObjList);

        return ResponseEntity.ok(deletedIds);
    }

    @Override
    @Transactional
    public ResponseEntity<List<FsRepo>> fundFsRepos(@PathVariable("fundId") Integer fundId) {
        ArrFund fund = arrangementService.getFund(fundId);

        List<ArrDigitalRepository> digitRepositories = externalSystemService.findDigitalRepository();

        List<FsRepo> result = null;
        if (CollectionUtils.isNotEmpty(digitRepositories)) {
            for (ArrDigitalRepository digiRepo : digitRepositories) {
                if (fileSystemRepoService.isFileSystemRepository(digiRepo)) {
                    Path repoPath = fileSystemRepoService.getPath(digiRepo, fund);
                    // append only real dirs
                    if(!Files.isDirectory(repoPath)) {
                        continue;
                    }
                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    FsRepo fsRepo = new FsRepo();
                    fsRepo.setFsRepoId(digiRepo.getExternalSystemId());
                    fsRepo.setName(digiRepo.getName());
                    fsRepo.setCode(digiRepo.getCode());
                    fsRepo.setPath(repoPath.toString());
                    result.add(fsRepo);
                }
            }
        }

        if (result == null) {
            result = Collections.emptyList();
        }
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> fundSetLevelMode(@PathVariable("fundId") Integer fundId,
    											 @PathVariable("nodeId") Integer nodeId,
    											 @RequestParam(value = "list", required = true) Boolean mode) {

        ArrNode node = arrangementService.getNode(nodeId);
        if (!node.getFundId().equals(fundId)) {
            throw new BusinessException("The node doesn't belong to the fund.", BaseCode.INVALID_STATE)
            	.set("nodeId", nodeId)
            	.set("fundId", fundId);
        }

        fundLevelService.setLevelMode(node, mode);

        return ResponseEntity.ok(null);
    }

    @Override
    @Transactional
    public ResponseEntity<FsItems> fundFsRepoItems(@PathVariable("fundId") Integer fundId,
                                                   @PathVariable("fsrepoId") Integer fsrepoId,
                                                   @RequestParam(value = "filterType", required = false) FsItemType filterType,
                                                   @RequestParam(value = "path", required = false) String path,
                                                   @RequestParam(value = "lastKey", required = false) String lastKey) {

        ArrFund fund = arrangementService.getFund(fundId);
        ArrDigitalRepository digiRepo = externalSystemService.getDigitalRepository(fsrepoId);

        Path itemPath = fileSystemRepoService.resolvePath(digiRepo, fund, path);
        if (!Files.isDirectory(itemPath)) {
            throw new BusinessException("Item is not directory.", BaseCode.INVALID_STATE)
                    .set("fsrepoId", fsrepoId)
                    .set("path", path)
                    .set("itemPath", itemPath);
        }

        int maxItems = 1000;
        if (digiRepo.getCode() != null) {
            // hack for debugging client
            if (digiRepo.getCode().endsWith("_DEBUG")) {
                maxItems = 2;
            }
        }
        int offset = 0;
        // check if continue in previous list
        if(lastKey!=null) {
            // lastKey is simply offset
            // this is just the basic implementation
            offset = Integer.parseInt(lastKey);
        }

        FsItems fsItems = new FsItems();

        Function<Path, Boolean> acceptor = prepareFSFilter(filterType);

        List<FsItem> fsItemList = new ArrayList<>();
        try (Stream<Path> ds = Files.walk(itemPath, 1);) {
            int counter = 0;
            Iterator<Path> it = ds.iterator();
            // skip first item - root
            it.next();
            // limit to 10k items
            while (it.hasNext() && counter < 10000) {
                Path item = it.next();
                if (acceptor.apply(item)) {
                    BasicFileAttributeView bfav = Files.getFileAttributeView(item, BasicFileAttributeView.class);

                    FsItem fsItem = new FsItem();
                    fsItem.setName(item.getFileName().toString());

                    BasicFileAttributes attrs = bfav.readAttributes();
                    if (attrs.isRegularFile()) {
                        fsItem.setItemType(FsItemType.FILE);
                        fsItem.setSize((int) attrs.size());
                    } else {
                        fsItem.setItemType(FsItemType.FOLDER);
                    }
                    OffsetDateTime odt = attrs.lastModifiedTime().toInstant().atOffset(ZoneOffset.UTC);
                    fsItem.setLastChange(odt);

                    fsItemList.add(fsItem);
                }
                counter++;
            }
            fsItemList.sort((c1, c2) -> {
                if (c1.getItemType().equals(FsItemType.FILE)) {
                    if (c2.getItemType().equals(FsItemType.FOLDER)) {
                        return 1;
                    }
                } else {
                    // c1 is folder
                    if (c2.getItemType().equals(FsItemType.FILE)) {
                        return -1;
                    }
                }
                return c1.getName().compareTo(c2.getName());
            });

            List<FsItem> appendItems;
            Integer nextOffset = null;
            // append selected items
            if ((fsItemList.size() - offset) <= maxItems) {
                // last items
                if (offset == 0) {
                    appendItems = fsItemList;
                } else {
                    appendItems = fsItemList.subList(offset, fsItemList.size());
                }
            } else {
                nextOffset = offset + maxItems;
                appendItems = fsItemList.subList(offset, nextOffset);
            }

            fsItems.getItems().addAll(appendItems);
            if (nextOffset != null) {
                fsItems.setLastKey(nextOffset.toString());
            }
        } catch (IOException ex) {
            throw new BusinessException("Failed to read.", ex, BaseCode.INVALID_STATE)
                    .set("fsrepoId", fsrepoId)
                    .set("path", path)
                    .set("itemPath", itemPath);
        }

        return ResponseEntity.ok(fsItems);
    }

    private Function<Path, Boolean> prepareFSFilter(FsItemType filterType) {
        if (filterType == null) {
            return p -> true;
        }
        switch (filterType) {
        	case FILE:
                return p -> Files.isRegularFile(p);
        	case FOLDER:
                return p -> Files.isDirectory(p);
        	default:
                throw new BusinessException("Invalid filter.", BaseCode.INVALID_STATE)
                        .set("filterType", filterType);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Resource> fundFsRepoItemData(@PathVariable("fundId") Integer fundId,
                                                     @PathVariable("fsrepoId") Integer fsrepoId,
                                                     @RequestParam(value = "path", required = true) String path) {
        ArrFund fund = arrangementService.getFund(fundId);
        ArrDigitalRepository digiRepo = externalSystemService.getDigitalRepository(fsrepoId);

        Path filePath = fileSystemRepoService.resolvePath(digiRepo, fund, path);

        FileSystemResource fsr = new FileSystemResource(filePath);
        return ResponseEntity.ok(fsr);
    }

    @Override
    @Transactional
    public ResponseEntity<Integer> fundFsCreateDAOLink(@PathVariable("fundId") Integer fundId,
                                                       @PathVariable("fsrepoId") Integer fsrepoId,
                                                       @PathVariable("nodeId") Integer nodeId,
                                                       @RequestParam(value = "path", required = false) String path) {
        ArrFund fund = arrangementService.getFund(fundId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFund(fund);
        ArrNode node = arrangementService.getNode(nodeId);

        ArrDigitalRepository digiRepo = externalSystemService.getDigitalRepository(fsrepoId);

        ArrDao dao = fileSystemRepoService.createDao(digiRepo, fundVersion, path);

        // create dao link in separate transaction
        // dao link might create level and data from levelTreeCache are available
        // in new transaction>
        ArrDaoLink daoLink = daoService.createDaoLink(fundVersion, dao, node);

        Objects.requireNonNull(daoLink);
        Objects.requireNonNull(daoLink.getDaoLinkId());
        Objects.requireNonNull(daoLink.getNodeId());

        return ResponseEntity.ok(daoLink.getDaoLinkId());
    }

    @Override
    @Transactional
    public ResponseEntity<List<UsedItemType>> fundUsedItemTypes(@PathVariable("fundId") Integer fundId,
                                                                @PathVariable("fundVersionId") Integer fundVersionId) {
    	Objects.requireNonNull(fundId);
    	Objects.requireNonNull(fundVersionId);

        ArrFundVersion fundVersion = arrangementService.getFundVersionById(fundVersionId);
        Validate.isTrue(fundVersion.getFundId().equals(fundId), "fundId does not match fundVersionId");

        List<UsedItemType> usedItemTypes = arrangementService.findUsedItemTypes(fundVersion);

        return ResponseEntity.ok(usedItemTypes);
    }
}
