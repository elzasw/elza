package cz.tacr.elza.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.controller.vo.BatchImportType;
import cz.tacr.elza.controller.vo.CreateBatchDescCsv;
import cz.tacr.elza.controller.vo.CreateBatchEdx;
import cz.tacr.elza.controller.vo.ImportBatch;
import cz.tacr.elza.controller.vo.ImportBatchDescCsv;
import cz.tacr.elza.controller.vo.ImportBatchEdx;
import cz.tacr.elza.controller.vo.ImportBatchPage;
import cz.tacr.elza.controller.vo.ImportItem;
import cz.tacr.elza.controller.vo.ImportItemError;
import cz.tacr.elza.controller.vo.ImportItemResult;
import cz.tacr.elza.controller.vo.ImpResultType;
import cz.tacr.elza.controller.vo.ItemState;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.ImpBatch;
import cz.tacr.elza.domain.ImpBatchDescCsv;
import cz.tacr.elza.domain.ImpBatchEdx;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ImpBatchService;
import cz.tacr.elza.service.ImpItemService;
import cz.tacr.elza.service.UserService;

@RestController
@RequestMapping("/api/v1")
public class ImportBatchController implements ImportBatchesApi {

    @Autowired
    private ImpBatchService batchService;
    @Autowired
    private ImpItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private ScopeRepository scopeRepository;
    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    private cz.tacr.elza.repository.ImpItemResultRepository itemResultRepository;

    // ---------- list / get ----------

    @Override
    public ResponseEntity<ImportBatchPage> importBatchList(Integer from, Integer count) {
        assertReadPermission();
        ImpBatchService.BatchPage page = batchService.list(nullSafe(from, 0), nullSafe(count, 10));
        ImportBatchPage vo = new ImportBatchPage();
        List<ImportBatch> items = new ArrayList<>(page.items().size());
        for (ImpBatch b : page.items()) {
            items.add(toVoBase(b));
        }
        vo.setItems(items);
        vo.setTotalCount((int) Math.min(page.totalCount(), Integer.MAX_VALUE));
        return ResponseEntity.ok(vo);
    }

    @Override
    public ResponseEntity<Object> importBatchGet(Integer id) {
        assertReadPermission();
        ImpBatch batch = batchService.findById(id);
        return ResponseEntity.ok(toVo(batch));
    }

    // ---------- create ----------

    @Override
    public ResponseEntity<ImportBatchEdx> importBatchCreateEdx(CreateBatchEdx params) {
        assertWritePermission();
        ImpBatchEdx batch = batchService.createEdxBatch(
                params.getName(),
                mapEnum(params.getFundImportStrategy(), cz.tacr.elza.domain.FundImportStrategy::valueOf),
                mapEnum(params.getFundPairKey(), cz.tacr.elza.domain.FundPairKey::valueOf),
                Boolean.TRUE.equals(params.getIgnoreRootNodes()),
                resolveScope(params.getScopeId()),
                Boolean.TRUE.equals(params.getSkipError()),
                Boolean.TRUE.equals(params.getKeepFiles()));
        return new ResponseEntity<>(toVoEdx(batch), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ImportBatchDescCsv> importBatchCreateCsv(CreateBatchDescCsv params) {
        assertWritePermission();
        ImpBatchDescCsv batch = batchService.createDescCsvBatch(
                params.getName(),
                params.getSeparator(),
                params.getEncoding(),
                Boolean.TRUE.equals(params.getSkipError()),
                Boolean.TRUE.equals(params.getKeepFiles()));
        return new ResponseEntity<>(toVoCsv(batch), HttpStatus.CREATED);
    }

    // ---------- update settings ----------

    @Override
    public ResponseEntity<ImportBatchEdx> importBatchUpdateEdx(Integer id, CreateBatchEdx params) {
        assertWritePermission();
        ImpBatchEdx batch = asEdx(batchService.findById(id));
        batch = batchService.updateEdxSettings(
                batch,
                params.getName(),
                mapEnum(params.getFundImportStrategy(), cz.tacr.elza.domain.FundImportStrategy::valueOf),
                mapEnum(params.getFundPairKey(), cz.tacr.elza.domain.FundPairKey::valueOf),
                Boolean.TRUE.equals(params.getIgnoreRootNodes()),
                resolveScope(params.getScopeId()),
                Boolean.TRUE.equals(params.getSkipError()),
                Boolean.TRUE.equals(params.getKeepFiles()));
        return ResponseEntity.ok(toVoEdx(batch));
    }

    @Override
    public ResponseEntity<ImportBatchDescCsv> importBatchUpdateCsv(Integer id, CreateBatchDescCsv params) {
        assertWritePermission();
        ImpBatchDescCsv batch = asCsv(batchService.findById(id));
        batch = batchService.updateCsvSettings(
                batch,
                params.getName(),
                params.getSeparator(),
                params.getEncoding(),
                Boolean.TRUE.equals(params.getSkipError()),
                Boolean.TRUE.equals(params.getKeepFiles()));
        return ResponseEntity.ok(toVoCsv(batch));
    }

    // ---------- delete ----------

    @Override
    public ResponseEntity<Void> importBatchRemove(Integer id) {
        assertWritePermission();
        batchService.delete(batchService.findById(id));
        return ResponseEntity.noContent().build();
    }

    // ---------- state actions ----------

    @Override
    public ResponseEntity<Void> importBatchStart(Integer id) {
        assertWritePermission();
        ImpBatch batch = batchService.findById(id);
        batchService.changeState(batch, BatchState.IN_PROGRESS);
        UsrUser me = userService.getLoggedUser();
        asyncRequestService.enqueue(batch, me == null ? null : me.getUserId());
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> importBatchDryRun(Integer id) {
        assertWritePermission();
        ImpBatch batch = batchService.findById(id);
        batchService.changeState(batch, BatchState.TEST_IN_PROGRESS);
        UsrUser me = userService.getLoggedUser();
        asyncRequestService.enqueue(batch, me == null ? null : me.getUserId());
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> importBatchPause(Integer id) {
        assertWritePermission();
        batchService.changeState(batchService.findById(id), BatchState.PAUSED);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> importBatchCancel(Integer id) {
        assertWritePermission();
        batchService.changeState(batchService.findById(id), BatchState.CANCELLED);
        return ResponseEntity.ok().build();
    }

    // ---------- items ----------

    @Override
    public ResponseEntity<List<ImportItem>> importBatchListItems(Integer id) {
        assertReadPermission();
        ImpBatch batch = batchService.findById(id);
        List<ImpItem> items = itemService.listByBatch(batch);
        List<ImportItem> vo = new ArrayList<>(items.size());
        for (ImpItem it : items) {
            vo.add(toVoItem(it));
        }
        return ResponseEntity.ok(vo);
    }

    @Override
    public ResponseEntity<Void> importBatchRemoveItem(Integer itemId) {
        assertWritePermission();
        itemService.deleteItem(itemService.findById(itemId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ImportItemError> importBatchItemError(Integer itemId) {
        assertReadPermission();
        ImpItem item = itemService.findById(itemId);
        if (item.getError() == null && item.getErrorDetail() == null) {
            return ResponseEntity.notFound().build();
        }
        ImportItemError vo = new ImportItemError();
        vo.setError(item.getError() == null ? "" : item.getError());
        vo.setErrorDetail(item.getErrorDetail() == null ? "" : item.getErrorDetail());
        return ResponseEntity.ok(vo);
    }

    @Override
    public ResponseEntity<List<ImportItemResult>> importBatchItemResults(Integer itemId) {
        assertReadPermission();
        ImpItem item = itemService.findById(itemId);
        List<ImportItemResult> vo = new ArrayList<>();
        for (cz.tacr.elza.domain.ImpItemResult r : itemService.listResults(item)) {
            vo.add(toVoResult(r));
        }
        return ResponseEntity.ok(vo);
    }

    // ---------- upload / download ----------

    @Override
    public ResponseEntity<ImportItem> importBatchUploadItem(Integer id, MultipartFile file) {
        assertWritePermission();
        ImpBatch batch = batchService.findById(id);
        try (InputStream in = file.getInputStream()) {
            ImpItem item = itemService.uploadItem(
                    batch,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    (int) Math.min(file.getSize(), Integer.MAX_VALUE),
                    in);
            return new ResponseEntity<>(toVoItem(item), HttpStatus.CREATED);
        } catch (IOException e) {
            throw new BusinessException("Cannot read uploaded file: " + e.getMessage(), BaseCode.INVALID_STATE);
        }
    }

    @Override
    public ResponseEntity<List<ImportItem>> importBatchUploadZip(Integer id, MultipartFile file) {
        assertWritePermission();
        ImpBatch batch = batchService.findById(id);
        try (InputStream in = file.getInputStream()) {
            List<ImpItem> added = itemService.uploadZip(batch, in);
            List<ImportItem> vo = new ArrayList<>(added.size());
            for (ImpItem it : added) {
                vo.add(toVoItem(it));
            }
            return new ResponseEntity<>(vo, HttpStatus.CREATED);
        } catch (IOException e) {
            throw new BusinessException("Cannot read uploaded ZIP: " + e.getMessage(), BaseCode.INVALID_STATE);
        }
    }

    @Override
    public ResponseEntity<List<cz.tacr.elza.controller.vo.ImportServerFolderEntry>> importBatchListServerFolder(String path) {
        assertWritePermission();
        try {
            List<ImpItemService.ServerFolderEntry> entries = itemService.listServerFolder(path);
            List<cz.tacr.elza.controller.vo.ImportServerFolderEntry> vo = new ArrayList<>(entries.size());
            for (ImpItemService.ServerFolderEntry e : entries) {
                cz.tacr.elza.controller.vo.ImportServerFolderEntry v = new cz.tacr.elza.controller.vo.ImportServerFolderEntry();
                v.setName(e.name());
                v.setDirectory(e.directory());
                v.setSize(e.size());
                vo.add(v);
            }
            return ResponseEntity.ok(vo);
        } catch (IOException ex) {
            throw new BusinessException("Cannot read server folder: " + ex.getMessage(), BaseCode.INVALID_STATE);
        }
    }

    @Override
    public ResponseEntity<List<ImportItem>> importBatchImportFolder(Integer id,
                                                                    cz.tacr.elza.controller.vo.ImportFromFolder params) {
        assertWritePermission();
        ImpBatch batch = batchService.findById(id);
        try {
            List<ImpItem> added = itemService.importFromServerFolder(batch, params.getPath(), params.getFiles());
            List<ImportItem> vo = new ArrayList<>(added.size());
            for (ImpItem it : added) {
                vo.add(toVoItem(it));
            }
            return new ResponseEntity<>(vo, HttpStatus.CREATED);
        } catch (IOException e) {
            throw new BusinessException("Cannot read server folder: " + e.getMessage(), BaseCode.INVALID_STATE);
        }
    }

    @Override
    public ResponseEntity<Resource> importBatchDownloadItem(Integer itemId) {
        assertReadPermission();
        ImpItem item = itemService.findById(itemId);
        InputStream in = itemService.openSource(item);
        if (in == null) {
            return ResponseEntity.notFound().build();
        }
        DmsFile dmsFile = item.getDmsFile();
        String name = dmsFile.getFileName() != null ? dmsFile.getFileName() : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name.replace("\"", "\\\"") + "\"")
                .contentType(MediaType.parseMediaType(
                        dmsFile.getMimeType() != null ? dmsFile.getMimeType() : "application/octet-stream"))
                .contentLength(dmsFile.getFileSize() == null ? -1 : dmsFile.getFileSize())
                .body((Resource) new InputStreamResource(in));
    }

    // ---------- permissions ----------

    private void assertReadPermission() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null) {
            throw new AccessDeniedException("Not authenticated", Collections.emptyList());
        }
        boolean ok = AuthorizationRequest.hasPermission(Permission.ADMIN).matches(userDetail)
                || (AuthorizationRequest.hasPermission(Permission.FUND_RD_ALL).matches(userDetail)
                        && AuthorizationRequest.hasPermission(Permission.AP_SCOPE_RD_ALL).matches(userDetail));
        if (!ok) {
            throw new AccessDeniedException(
                    "Reading import batches requires ADMIN or (FUND_RD_ALL + AP_SCOPE_RD_ALL)",
                    Collections.emptyList());
        }
    }

    private void assertWritePermission() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null) {
            throw new AccessDeniedException("Not authenticated", Collections.emptyList());
        }
        boolean ok = AuthorizationRequest.hasPermission(Permission.ADMIN).matches(userDetail)
                || (AuthorizationRequest.hasPermission(Permission.FUND_ARR_ALL).matches(userDetail)
                        && AuthorizationRequest.hasPermission(Permission.AP_SCOPE_WR_ALL).matches(userDetail));
        if (!ok) {
            throw new AccessDeniedException(
                    "Managing import batches requires ADMIN or (FUND_ARR_ALL + AP_SCOPE_WR_ALL)",
                    Collections.emptyList());
        }
    }

    // ---------- helpers ----------

    private ApScope resolveScope(Integer scopeId) {
        return scopeId == null ? null : scopeRepository.findById(scopeId).orElseThrow(() ->
                new BusinessException("Scope not found: " + scopeId, BaseCode.ID_NOT_EXIST));
    }

private static <D, V extends Enum<V>> D mapEnum(V vo, java.util.function.Function<String, D> factory) {
        return vo == null ? null : factory.apply(vo.name());
    }

    private static int nullSafe(Integer v, int fallback) {
        return v == null ? fallback : v;
    }

    private static ImpBatchEdx asEdx(ImpBatch batch) {
        if (!(batch instanceof ImpBatchEdx edx)) {
            throw new BusinessException(
                    "Batch " + batch.getBatchId() + " is not an EDX2 batch",
                    BaseCode.INVALID_STATE);
        }
        return edx;
    }

    private static ImpBatchDescCsv asCsv(ImpBatch batch) {
        if (!(batch instanceof ImpBatchDescCsv csv)) {
            throw new BusinessException(
                    "Batch " + batch.getBatchId() + " is not a CSV batch",
                    BaseCode.INVALID_STATE);
        }
        return csv;
    }

    // ---------- mapping domain -> VO ----------

    private static Object toVo(ImpBatch b) {
        if (b instanceof ImpBatchEdx edx) return toVoEdx(edx);
        if (b instanceof ImpBatchDescCsv csv) return toVoCsv(csv);
        return toVoBase(b);
    }

    private static ImportBatch toVoBase(ImpBatch b) {
        ImportBatch vo = new ImportBatch();
        fillBase(vo, b);
        return vo;
    }

    private static ImportBatchEdx toVoEdx(ImpBatchEdx b) {
        ImportBatchEdx vo = new ImportBatchEdx();
        fillEdx(vo, b);
        return vo;
    }

    private static ImportBatchDescCsv toVoCsv(ImpBatchDescCsv b) {
        ImportBatchDescCsv vo = new ImportBatchDescCsv();
        fillCsv(vo, b);
        return vo;
    }

    private static void fillBase(ImportBatch vo, ImpBatch b) {
        vo.setBatchId(b.getBatchId());
        vo.setImportType(BatchImportType.valueOf(b.getImportType().name()));
        vo.setName(b.getName());
        vo.setCreatedAt(b.getCreatedAt());
        vo.setCreatedByUserId(b.getCreatedByUser() == null ? null : b.getCreatedByUser().getUserId());
        vo.setExecutedAt(b.getExecutedAt());
        vo.setExecutedByUserId(b.getExecutedByUser() == null ? null : b.getExecutedByUser().getUserId());
        vo.setState(cz.tacr.elza.controller.vo.BatchState.valueOf(b.getState().name()));
        vo.setLastStateChangeAt(b.getLastStateChangeAt());
        vo.setSkipError(b.isSkipError());
        vo.setKeepFiles(b.isKeepFiles());
        vo.setFilesDeletedAt(b.getFilesDeletedAt());
    }

    private static void fillEdx(ImportBatchEdx vo, ImpBatchEdx b) {
        vo.setBatchId(b.getBatchId());
        vo.setImportType(BatchImportType.EDX2);
        vo.setName(b.getName());
        vo.setCreatedAt(b.getCreatedAt());
        vo.setCreatedByUserId(b.getCreatedByUser() == null ? null : b.getCreatedByUser().getUserId());
        vo.setExecutedAt(b.getExecutedAt());
        vo.setExecutedByUserId(b.getExecutedByUser() == null ? null : b.getExecutedByUser().getUserId());
        vo.setState(cz.tacr.elza.controller.vo.BatchState.valueOf(b.getState().name()));
        vo.setLastStateChangeAt(b.getLastStateChangeAt());
        vo.setSkipError(b.isSkipError());
        vo.setKeepFiles(b.isKeepFiles());
        vo.setFilesDeletedAt(b.getFilesDeletedAt());
        vo.setFundImportStrategy(cz.tacr.elza.controller.vo.FundImportStrategy.valueOf(b.getFundImportStrategy().name()));
        vo.setFundPairKey(b.getFundPairKey() == null ? null
                : cz.tacr.elza.controller.vo.FundPairKey.valueOf(b.getFundPairKey().name()));
        vo.setIgnoreRootNodes(b.isIgnoreRootNodes());
        vo.setScopeId(b.getScope() == null ? null : b.getScope().getScopeId());
    }

    private static void fillCsv(ImportBatchDescCsv vo, ImpBatchDescCsv b) {
        vo.setBatchId(b.getBatchId());
        vo.setImportType(BatchImportType.ADD_DESC_ITEMS_CSV);
        vo.setName(b.getName());
        vo.setCreatedAt(b.getCreatedAt());
        vo.setCreatedByUserId(b.getCreatedByUser() == null ? null : b.getCreatedByUser().getUserId());
        vo.setExecutedAt(b.getExecutedAt());
        vo.setExecutedByUserId(b.getExecutedByUser() == null ? null : b.getExecutedByUser().getUserId());
        vo.setState(cz.tacr.elza.controller.vo.BatchState.valueOf(b.getState().name()));
        vo.setLastStateChangeAt(b.getLastStateChangeAt());
        vo.setSkipError(b.isSkipError());
        vo.setKeepFiles(b.isKeepFiles());
        vo.setFilesDeletedAt(b.getFilesDeletedAt());
        vo.setSeparator(b.getSeparator());
        vo.setEncoding(b.getEncoding());
    }

    private ImportItem toVoItem(ImpItem it) {
        ImportItem vo = new ImportItem();
        vo.setItemId(it.getItemId());
        vo.setBatchId(it.getBatch().getBatchId());
        vo.setDmsFileId(it.getDmsFile() == null ? null : it.getDmsFile().getFileId());
        vo.setItemName(it.getItemName());
        vo.setState(ItemState.valueOf(it.getState().name()));
        vo.setExecOrder(it.getExecOrder());
        vo.setCreatedAt(it.getCreatedAt());
        vo.setCreatedByUserId(it.getCreatedByUser() == null ? null : it.getCreatedByUser().getUserId());
        vo.setExecutedAt(it.getExecutedAt());
        vo.setExecutedByUserId(it.getExecutedByUser() == null ? null : it.getExecutedByUser().getUserId());
        vo.setFinishedAt(it.getFinishedAt());
        vo.setError(it.getError());
        vo.setFundId(it.getFund() == null ? null : it.getFund().getFundId());
        vo.setResultFundIds(itemResultRepository.findDistinctFundIdsByItem(it.getItemId()));
        vo.setNodesCreated(it.getNodesCreated());
        vo.setNodesUpdated(it.getNodesUpdated());
        vo.setApsCreated(it.getApsCreated());
        vo.setApsPaired(it.getApsPaired());
        return vo;
    }

    private static ImportItemResult toVoResult(cz.tacr.elza.domain.ImpItemResult r) {
        ImportItemResult vo = new ImportItemResult();
        vo.setItemResultId(r.getItemResultId());
        vo.setResultType(ImpResultType.valueOf(r.getResultType().name()));
        vo.setFundId(r.getFund() == null ? null : r.getFund().getFundId());
        vo.setFundChangeId(r.getFundChange() == null ? null : r.getFundChange().getChangeId());
        vo.setApChangeId(r.getApChange() == null ? null : r.getApChange().getChangeId());
        return vo;
    }
}
