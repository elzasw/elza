package cz.tacr.elza.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.common.FactoryUtils;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.BulkActionRunState;
import cz.tacr.elza.controller.vo.CreateFund;
import cz.tacr.elza.controller.vo.FundsActionGroupRequest;
import cz.tacr.elza.controller.vo.FundsActionGroupResult;
import cz.tacr.elza.controller.vo.FundsChangeRun;
import cz.tacr.elza.controller.vo.MultiFundActionRequest;
import cz.tacr.elza.controller.vo.MultiFundActionResult;
import cz.tacr.elza.controller.vo.FindFundsResult;
import cz.tacr.elza.controller.vo.FsItemSortType;
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
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.dataexchange.output.IOExportFundsCsv;
import cz.tacr.elza.dataexchange.output.IOExportWorker;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AdminPermissionUpdateMode;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ArrangementService.FindFundVersionsResult;
import cz.tacr.elza.service.DaoService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
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
    private FileSystemRepoBrowser fileSystemRepoBrowser;

    @Autowired
    private FileSystemRepoService fileSystemRepoService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private DaoService daoService;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private BulkActionService bulkActionService;

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

        int id = ioExportWorker.enqueue(requestId -> new IOExportFundsCsv(userId, requestId, downloadFileName, searchParams));
        return ResponseEntity.ok(id);
    }

    // POST /action/funds/group
    @Override
    @Transactional
    public ResponseEntity<FundsActionGroupResult> bulkActionGroupFundsByRuleSet(@RequestBody FundsActionGroupRequest request) {
        // bez fundIds se použije filtr; prázdný filtr znamená všechny fondy
        return ResponseEntity.ok(bulkActionService.groupFundsByRuleSet(request.getFundIds(), request.getFilters()));
    }

    // POST /action/queue-multi
    @Override
    @Transactional
    public ResponseEntity<MultiFundActionResult> bulkActionQueueMultiFundAction(@RequestBody MultiFundActionRequest request) {
        Validate.isTrue(StringUtils.isNotBlank(request.getCode()), "Kód musí být vyplněn");
        Validate.notNull(request.getRuleSetId(), "Musí být vyplněn identifikátor pravidel");
        UsrUser user = userService.getLoggedUser();
        Integer userId = user == null ? null : user.getUserId();
        return ResponseEntity.ok(bulkActionService.queueMulti(userId, request.getCode(), request.getRuleSetId(),
                request.getFundIds(), request.getFilters()));
    }

    // GET /action/funds-change/{fundsChangeId}
    @Override
    @Transactional
    public ResponseEntity<List<FundsChangeRun>> bulkActionGetFundsChangeRuns(@PathVariable("fundsChangeId") Integer fundsChangeId) {
        List<FundsChangeRun> runs = bulkActionService.getRunsByFundsChange(fundsChangeId).stream()
                .map(run -> toFundsChangeRun(run))
                .collect(Collectors.toList());
        return ResponseEntity.ok(runs);
    }

    private FundsChangeRun toFundsChangeRun(final ArrBulkActionRun run) {
        return new FundsChangeRun(run.getBulkActionRunId(), run.getFundVersionId(), run.getBulkActionCode(),
                BulkActionRunState.fromValue(run.getState().name()))
                .datePlanned(toOffsetDateTime(run.getDatePlanned()))
                .dateStarted(toOffsetDateTime(run.getDateStarted()))
                .dateFinished(toOffsetDateTime(run.getDateFinished()))
                .error(run.getError());
    }

    private static OffsetDateTime toOffsetDateTime(final Date date) {
        return date == null ? null : date.toInstant().atOffset(ZoneOffset.UTC);
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

    // PUT /fund/{id}
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

        ArrFundVersion fundVersion = arrangementService.updateFund(arrFund, ruleSet, apScopes, null, null, AdminPermissionUpdateMode.NO_SYNC);
        ArrNode rootNode = fundVersion.getRootNode();

        return ResponseEntity.ok(factoryVo.createFundDetail(fundVersion.getFund(), rootNode.getUuid()));
    }

    // GET /fund/{fundId}/fsrepos
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<FsRepo>> fundFsRepos(@PathVariable("fundId") Integer fundId) {
        ArrFund fund = arrangementService.getFund(fundId);
        List<ArrDigitalRepository> digitalRepositories = externalSystemService.findDigitalRepository();
        List<FsRepo> result = fileSystemRepoBrowser.listRepos(fund, digitalRepositories);

        return ResponseEntity.ok(result);
    }

    // POST /fund/{fundId}/node/{nodeId}/mode
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

    // GET /fund/{fundId}/fsrepo/{fsrepoId}/items
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<FsItems> fundFsRepoItems(@PathVariable("fundId") Integer fundId,
                                                   @PathVariable("fsrepoId") Integer fsrepoId,
                                                   @RequestParam(value = "filterType", required = false) @Nullable FsItemType filterType,
                                                   @RequestParam(value = "path", required = false) @Nullable String path,
                                                   @RequestParam(value = "lastKey", required = false) @Nullable String lastKey,
                                                   @RequestParam(value = "sortingType", required = false) @Nullable FsItemSortType sortingType,
                                                   @RequestParam(value = "fileFilter", required = false) @Nullable String fileFilter) {

        ArrFund fund = arrangementService.getFund(fundId);
        ArrDigitalRepository digiRepo = externalSystemService.getDigitalRepository(fsrepoId);
        try {
            FsItems result = fileSystemRepoBrowser.browseItems(digiRepo, fund, path, filterType, lastKey, sortingType, fileFilter);
            return ResponseEntity.ok(result);
        } catch (IOException ex) {
            throw new BusinessException("Failed to read.", ex, BaseCode.INVALID_STATE)
                    .set("fsrepoId", fsrepoId)
                    .set("path", path);
        }
    }

    // GET /fund/{fundId}/fsrepo/{fsrepoId}/item-data
    @Override
    @Transactional(readOnly = true)
    @AuthMethod(permission = { Permission.ADMIN, Permission.FUND_RD_ALL, Permission.FUND_RD })
    public ResponseEntity<Resource> fundFsRepoItemData(@AuthParam(type = AuthParam.Type.FUND) @PathVariable("fundId") Integer fundId,
                                                       @PathVariable("fsrepoId") Integer fsrepoId,
                                                       @RequestParam(value = "path", required = true) String path) {
        ArrFund fund = arrangementService.getFund(fundId);
        ArrDigitalRepository digiRepo = externalSystemService.getDigitalRepository(fsrepoId);

        Path filePath = fileSystemRepoService.resolvePath(digiRepo, fund, path);

        String contentType = fileSystemRepoService.getMimetype(filePath);
        if (StringUtils.isEmpty(contentType)) {
            contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (!FileSystemRepoService.isInlineRenderable(contentType)) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(filePath.getFileName().toString())
                    .build());
        }

        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(filePath));
    }

    // PUT /fund/{fundId}/fsrepo/{fsrepoId}/linkitem/{nodeId}
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

    // GET /fund/{fundId}/usedItemtypes/{fundVersionId}
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
