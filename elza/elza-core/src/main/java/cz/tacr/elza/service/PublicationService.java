package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.vo.AvailablePublication;
import cz.tacr.elza.controller.vo.AvailablePublications;
import cz.tacr.elza.controller.vo.PublicationDetail;
import cz.tacr.elza.controller.vo.PublicationList;
import cz.tacr.elza.controller.vo.PublicationReportStatus;
import cz.tacr.elza.controller.vo.PublicationStateInternal;
import cz.tacr.elza.controller.vo.PublicationStatusReport;
import cz.tacr.elza.controller.vo.UserRef;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlNameConsts;
import cz.tacr.elza.domain.ArrExport;
import cz.tacr.elza.domain.ArrExport.State;
import cz.tacr.elza.domain.ArrExportType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ConflictException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ExportRepository;
import cz.tacr.elza.repository.ExportTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;

/**
 * Manages fund-scoped publication records ({@code arr_export}).
 *
 * Real XML preparation is out of scope: {@link #create(Integer, Integer)} and
 * {@link #copy(Integer, Integer, Integer)} only persist a new row in state
 * {@link ArrExport.State#NEW}; an asynchronous worker (added later) is
 * expected to transition it to {@link ArrExport.State#PREPARED} once the XML
 * file has been generated.
 */
@Service
public class PublicationService {

    /** Hard cap to keep the listing endpoint cheap, mirrors the OpenAPI default. */
    private static final int MAX_PAGE_SIZE = 200;

    private final ExportRepository exportRepository;
    private final ExportTypeRepository exportTypeRepository;
    private final FundVersionRepository fundVersionRepository;
    private final DmsService dmsService;
    private final UserService userService;
    private final AsyncRequestService asyncRequestService;

    /** Public-API page size cap; matches the contract. */
    private static final int PUBLIC_API_PAGE_SIZE = 100;

    /** Default cursor returned when caller omits {@code lastTransaction} and the result is empty. */
    private static final String EMPTY_CURSOR = "0";

    @Autowired
    public PublicationService(final ExportRepository exportRepository,
                              final ExportTypeRepository exportTypeRepository,
                              final FundVersionRepository fundVersionRepository,
                              final DmsService dmsService,
                              final UserService userService,
                              final AsyncRequestService asyncRequestService) {
        this.exportRepository = exportRepository;
        this.exportTypeRepository = exportTypeRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.dmsService = dmsService;
        this.userService = userService;
        this.asyncRequestService = asyncRequestService;
    }

    @Transactional(readOnly = true)
	public AvailablePublications listAvailable(final String targetSystem, final String lastTransaction) {
        ArrExportType type = requireActiveType(targetSystem);

        Long lastSeq = parseCursor(lastTransaction);

        List<ArrExport> rows = exportRepository.findAvailable(type.getCode(), lastSeq, PageRequest.of(0, PUBLIC_API_PAGE_SIZE));

        // Per spec: when several prepared exports exist for the same fund,
        // expose only the most recent one. Rows are already ordered by exportSeq ASC,
        // so a LinkedHashMap keyed by fundId keeps the highest seq per fund while
        // preserving cursor order.
        Map<Integer, ArrExport> dedupedByFund = new LinkedHashMap<>();
        for (ArrExport e : rows) {
            dedupedByFund.put(e.getFundVersion().getFundId(), e);
        }

        AvailablePublications result = new AvailablePublications();
        result.setItems(dedupedByFund.values().stream()
                .map(this::toAvailableVO)
                .collect(Collectors.toList()));

        // Advance the cursor by the last raw row (not the deduplicated one) so the
        // skipped duplicates are not replayed on the next call.
        String nextCursor = rows.isEmpty()
                ? (lastTransaction != null ? lastTransaction : EMPTY_CURSOR)
                : Long.toString(rows.get(rows.size() - 1).getExportSeq());
        result.setNextTransaction(nextCursor);
        return result;
    }

    @Transactional
	public DownloadPayload downloadAvailable(final Integer publicationId) {
        ArrExport export = exportRepository.findById(publicationId)
                .orElseThrow(() -> new ObjectNotFoundException("Publication not found", BaseCode.ID_NOT_EXIST).setId(publicationId));

        // Hide internal-only states from the public API (NEW / PREPARE_ERROR / INVALIDATED).
        State state = export.getState();
        if (state != State.PREPARED && state != State.FETCHED
                && state != State.PUBLISHED && state != State.PUBLISH_ERROR) {
            throw new ObjectNotFoundException(
                    "Publication not found", BaseCode.ID_NOT_EXIST).setId(publicationId);
        }

        requireActiveType(export.getExportType().getCode());

        DmsFile file = export.getFile();
        if (file == null) {
            // 410 Gone — retention sweep already deleted the file.
            // Controller maps a null payload to HttpStatus.GONE.
            return null;
        }

        // First successful fetch: PREPARED → FETCHED, stamp lastFetchedAt.
        OffsetDateTime now = OffsetDateTime.now();
        if (state == State.PREPARED) {
            export.setState(State.FETCHED);
        }
        export.setLastFetchedAt(now);
        exportRepository.save(export);

        Resource resource = new FileSystemResource(dmsService.getFilePath(file));
        return new DownloadPayload(resource, buildFileName(export));
	}

    @Transactional
	public void reportStatus(final Integer publicationId, final PublicationStatusReport report) {
        ArrExport export = exportRepository.findById(publicationId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Publication not found", BaseCode.ID_NOT_EXIST).setId(publicationId));

        State state = export.getState();
        if (state == State.NEW || state == State.PREPARE_ERROR || state == State.INVALIDATED) {
            // Internal-only states are not exposed.
            throw new ObjectNotFoundException(
                    "Publication not found", BaseCode.ID_NOT_EXIST).setId(publicationId);
        }

        // Last-writer-wins: a publication system that gets resynchronised may
        // report the same publication repeatedly. Identical replays are no-ops;
        // changed values silently overwrite. We can layer rate-limiting or
        // conflict detection on top later if it becomes necessary.
        OffsetDateTime reportedAt = report.getPublishedAt();
        if (report.getStatus() == PublicationReportStatus.OK) {
            export.setState(State.PUBLISHED);
            export.setPublishedAt(reportedAt);
            export.setErrorMessage(null);
            export.setErrorAt(null);
        } else { // ERROR
            export.setState(State.PUBLISH_ERROR);
            export.setErrorAt(reportedAt);
            export.setErrorMessage(report.getErrorMessage());
        }
        exportRepository.save(export);
    }

    @Transactional(readOnly = true)
    public PublicationList listByFund(final Integer fundId,
                                      final Integer publicationTypeId,
                                      final Integer offset,
                                      final Integer limit) {
        // No open-version requirement: viewing the publication history of a
        // locked fund is still meaningful. A non-existent fundId simply
        // returns an empty page.

        int effectiveOffset = offset == null || offset < 0 ? 0 : offset;
        int effectiveLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, MAX_PAGE_SIZE);

        // Spring Data Pageable wants page index, not offset — convert.
        Pageable pageable = PageRequest.of(effectiveOffset / effectiveLimit, effectiveLimit);
        if (effectiveOffset % effectiveLimit != 0) {
            // Fall back to raw offset semantics when the caller picks a non-aligned page.
            pageable = new RawOffsetPageable(effectiveOffset, effectiveLimit);
        }

        List<ArrExport> rows = exportRepository.findFundExports(fundId, publicationTypeId, pageable);
        long total = exportRepository.countFundExports(fundId, publicationTypeId);

        Set<Integer> userIds = rows.stream()
                .map(ArrExport::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, UserRef> userRefs = userService.toUserRefMap(userIds);

        PublicationList result = new PublicationList();
        result.setItems(rows.stream()
                .map(r -> toVO(r, userRefs.get(r.getUserId())))
                .collect(Collectors.toList()));
        result.setTotalCount(Math.toIntExact(total));
        return result;
    }

    @Transactional
    public PublicationDetail create(final Integer fundId, final Integer publicationTypeId) {
        ArrFundVersion fundVersion = requireOpenVersion(fundId);
        ArrExportType type = requireActiveTypeById(publicationTypeId);
        authorizePublishToType(type, fundId);

        if (exportRepository.countOutstanding(fundVersion.getFundVersionId(), publicationTypeId) > 0) {
            // Only NEW blocks: a publication already queued for async
            // preparation must finish first (avoids racing two identical
            // generations). PREPARED does NOT block — the fund may have
            // changed and the new publication carries different content.
            throw new ConflictException("Pending publication already exists for this fund and type", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("fundVersionId", fundVersion.getFundVersionId())
                    .set("publicationTypeId", publicationTypeId);
        }

        ArrExport export = new ArrExport();
        export.setExportType(type);
        export.setFundVersion(fundVersion);
        export.setExportFilter(type.getExportFilter());
        export.setState(ArrExport.State.NEW);
        export.setCreatedAt(OffsetDateTime.now());
        export.setUser(requireLoggedUser());
        export = exportRepository.save(export);

        asyncRequestService.enqueue(fundVersion, export, export.getUserId());

        return toVO(export);
    }

    @Transactional(readOnly = true)
    public DownloadPayload download(final Integer fundId, final Integer publicationId) {
        ArrExport export = requireExportInFund(fundId, publicationId);
        DmsFile file = export.getFile();
        if (file == null) {
            // 404 — no file produced yet (NEW, PREPARE_ERROR) or removed by retention.
            throw new ObjectNotFoundException("Publication has no downloadable file",
                    BaseCode.ID_NOT_EXIST).setId(publicationId);
        }
        Resource resource = new FileSystemResource(dmsService.getFilePath(file));
        return new DownloadPayload(resource, buildFileName(export));
    }

    @Transactional
    public void invalidate(final Integer fundId, final Integer publicationId) {
        ArrExport export = requireExportInFund(fundId, publicationId);
        // Invalidation uses the same permission model as publishing into the
        // type — the type's allowPerm* flags decide which permission family
        // is accepted; ADMIN / FUND_ADMIN always pass.
        authorizePublishToType(export.getExportType(), fundId);
        if (export.getState() == ArrExport.State.INVALIDATED) {
            throw new ConflictException("Publication is already invalidated",
                    BaseCode.INVALID_STATE).set("id", publicationId);
        }
        DmsFile file = export.getFile();
        // Null the FK before deleting the dms_file row — arr_export does not cascade.
        export.setFile(null);
        export.setState(ArrExport.State.INVALIDATED);
        export.setInvalidatedAt(OffsetDateTime.now());
        exportRepository.save(export);
        deleteFileIfUnreferenced(file, publicationId);
    }

    /**
     * Verify the logged-in user is allowed to publish into the given type on
     * the given fund. Used by {@link #create}, {@link #copy} and
     * {@link #invalidate}.
     *
     * Access is granted when any of these holds:
     * <ul>
     *   <li>user has {@link Permission#ADMIN} (super-admin),</li>
     *   <li>user has {@link Permission#FUND_ADMIN} (fund administration,
     *       bypasses the per-type flags),</li>
     *   <li>{@code type.allowPermExport == true} AND user has
     *       {@link Permission#FUND_EXPORT_ALL} or
     *       {@link Permission#FUND_EXPORT} for this fund,</li>
     *   <li>{@code type.allowPermPublication == true} AND user has
     *       {@link Permission#FUND_PUBLISH_ALL} or
     *       {@link Permission#FUND_PUBLISH} for this fund.</li>
     * </ul>
     *
     * Otherwise throws {@link AccessDeniedException} (HTTP 403). The
     * publication type's {@code allowPerm*} flags dictate which permission
     * families are accepted; if both flags are set, either is sufficient.
     */
    private void authorizePublishToType(final ArrExportType type, final Integer fundId) {
        if (userService.hasPermission(Permission.ADMIN)) {
            return;
        }
        if (userService.hasPermission(Permission.FUND_ADMIN)) {
            return;
        }

        boolean exportAllowed = Boolean.TRUE.equals(type.getAllowPermExport());
        boolean publishAllowed = Boolean.TRUE.equals(type.getAllowPermPublication());

        if (exportAllowed) {
            if (userService.hasPermission(Permission.FUND_EXPORT_ALL)
                    || userService.hasPermission(Permission.FUND_EXPORT, fundId)) {
                return;
            }
        }
        if (publishAllowed) {
            if (userService.hasPermission(Permission.FUND_PUBLISH_ALL)
                    || userService.hasPermission(Permission.FUND_PUBLISH, fundId)) {
                return;
            }
        }

        // Report which permissions would have unlocked the operation —
        // helps the caller diagnose configuration issues.
        List<Permission> tried = new ArrayList<>();
        tried.add(Permission.ADMIN);
        tried.add(Permission.FUND_ADMIN);
        if (exportAllowed) {
            tried.add(Permission.FUND_EXPORT_ALL);
            tried.add(Permission.FUND_EXPORT);
        }
        if (publishAllowed) {
            tried.add(Permission.FUND_PUBLISH_ALL);
            tried.add(Permission.FUND_PUBLISH);
        }
        throw new AccessDeniedException(
                "Missing permission to publish into type " + type.getCode() + " on fund " + fundId,
                tried.toArray(new Permission[0]));
    }

    /**
     * Apply the retention policy of {@code publicationTypeId} for the given
     * {@code fundId}. Files of exports that fall outside the retention window
     * are removed; the {@code arr_export} rows themselves are preserved as an
     * audit trail.
     *
     * Retention is scoped per fund + type: each fund maintains its own
     * publication history independently of other funds' publications of the
     * same type. A retention count of {@code 0} means unlimited (no sweep).
     *
     * Idempotent — calling repeatedly without intervening publications is a
     * no-op (already-swept rows have {@code file_id IS NULL} and are excluded
     * from the candidate set).
     *
     * Intended caller: the async generator worker, after a successful
     * transition to {@link ArrExport.State#PREPARED}. Runs in its own
     * transaction so a sweep failure cannot roll back a successful
     * generation.
     *
     * @param fundId              archival fund whose publications to sweep
     * @param publicationTypeId   publication type whose retention applies
     */
    @Transactional
    public void sweepRetention(final Integer fundId, final Integer publicationTypeId) {
        ArrExportType type = exportTypeRepository.findById(publicationTypeId).orElse(null);
        if (type == null) {
            // Type was removed between the export's preparation and this sweep.
            // Nothing to enforce — leave the row(s) alone.
            return;
        }
        Integer retention = type.getRetentionCount();
        if (retention == null || retention <= 0) {
            // 0 (or null, defensively) = unlimited retention.
            return;
        }

        List<ArrExport> retained = exportRepository.findRetentionExportsForFundAndType(
                fundId, publicationTypeId);
        if (retained.size() <= retention) {
            return;
        }

        // Newest retention rows are kept; the remainder is the sweep set.
        for (int i = retention; i < retained.size(); i++) {
            ArrExport candidate = retained.get(i);
            DmsFile file = candidate.getFile();
            candidate.setFile(null);
            exportRepository.save(candidate);
            // Honours sharing: if a copy() linked another export to the same
            // file, the underlying row survives; only this export's link
            // disappears.
            deleteFileIfUnreferenced(file, candidate.getExportId());
        }
    }

    /**
     * Physically delete a {@code dms_file} only if no other {@code arr_export}
     * still references it.
     *
     * Sharing happens via {@link #copy(Integer, Integer, Integer)}, which
     * links a new export to the source's file rather than regenerating the
     * XML. Both this method and a future retention sweep must use this
     * helper instead of calling {@link DmsService#deleteFile(DmsFile)}
     * directly.
     *
     * @param file              the file the caller would like to drop; null is a no-op
     * @param ownerExportId     the export ID that owned this file before the caller
     *                          nulled its reference — excluded from the share check
     */
    private void deleteFileIfUnreferenced(final DmsFile file, final Integer ownerExportId) {
        if (file == null) {
            return;
        }
        long others = exportRepository.countOtherReferencingFile(
                file.getFileId(), ownerExportId);
        if (others == 0) {
            dmsService.deleteFile(file);
        }
    }

    /**
     * Copy an existing prepared publication into another target system.
     *
     * Re-links the SAME {@link DmsFile} — the XML is not regenerated and no
     * async work is queued. The new {@code arr_export} row is created directly
     * in {@link ArrExport.State#PREPARED} with its own {@code exportSeq}, so it
     * is immediately available to the public publication API of the target
     * system.
     */
    @Transactional
    public PublicationDetail copy(final Integer fundId,
                                  final Integer publicationId,
                                  final Integer targetPublicationTypeId) {
        ArrExport source = requireExportInFund(fundId, publicationId);
        ArrExportType targetType = requireActiveTypeById(targetPublicationTypeId);
        // Permission is evaluated against the TARGET type — that's the type
        // we'll publish into. The source's type only gates listing/download
        // (already enforced upstream).
        authorizePublishToType(targetType, fundId);

        // Source must have a downloadable XML file. The file is null in
        // NEW (not yet prepared), PREPARE_ERROR (preparation failed),
        // INVALIDATED (user removed it), or after retention has swept it.
        DmsFile sourceFile = source.getFile();
        if (sourceFile == null) {
            throw new ObjectNotFoundException("Source publication has no downloadable file",
                    BaseCode.ID_NOT_EXIST).setId(publicationId);
        }

        // Compare the source's filter SNAPSHOT (frozen at create time, stored on
        // arr_export) against the target type's CURRENT filter. The whole reason
        // arr_export.export_filter_id exists is so the source export's filter is
        // preserved even when the source type's configuration changes; comparing
        // against source.getExportType().getExportFilterId() would silently let
        // through copies that produce different content from the source.
        Integer sourceFilterId = source.getExportFilterId();
        Integer targetFilterId = targetType.getExportFilterId();
        if (!Objects.equals(sourceFilterId, targetFilterId)) {
            throw new ConflictException(
                    "Source publication and target publication type use incompatible filter settings",
                    BaseCode.INVALID_STATE)
                    .set("sourcePublicationId", source.getExportId())
                    .set("targetTypeId", targetPublicationTypeId);
        }

        // Re-link the source's DMS file directly — no regeneration, no async
        // work. The new row goes straight to PREPARED with its own exportSeq.
        //
        // Data-lifecycle fields (file, exportFilter, lastChange, preparedAt)
        // are inherited from source: they describe the XML payload, which is
        // unchanged. Row-lifecycle fields (createdAt, exportSeq, user) are
        // fresh — this is a new record.
        //
        // Because the dms_file is now shared, any code that deletes it must
        // use deleteFileIfUnreferenced(). invalidate() already does; the
        // retention sweep (TBD) must follow the same rule.
        ArrExport copy = new ArrExport();
        copy.setExportType(targetType);
        copy.setFundVersion(source.getFundVersion());
        copy.setExportFilter(source.getExportFilter());
        copy.setFile(sourceFile);
        copy.setLastChange(source.getLastChange());
        copy.setState(ArrExport.State.PREPARED);
        copy.setExportSeq(exportRepository.nextExportSeq());
        copy.setCreatedAt(OffsetDateTime.now());
        copy.setPreparedAt(source.getPreparedAt());
        copy.setUser(requireLoggedUser());
        copy = exportRepository.save(copy);

        return toVO(copy);
    }

    /**
     * Single-record path: resolves the {@code createdBy} UserRef inline. Each
     * call costs one extra query; do not use in list loops — use
     * {@link #toVO(ArrExport, UserRef)} with a pre-built map instead.
     */
    public PublicationDetail toVO(final ArrExport export) {
        UserRef createdBy = export.getUser() != null ? userService.toUserRef(export.getUser()) : null;
        return toVO(export, createdBy);
    }

    public PublicationDetail toVO(final ArrExport export, final UserRef createdBy) {
        PublicationDetail vo = new PublicationDetail();
        vo.setId(export.getExportId());
        ArrExportType type = export.getExportType();
        vo.setTypeId(type.getExportTypeId());
        vo.setTypeCode(type.getCode());
        vo.setTypeName(type.getName());
        vo.setFundVersionId(export.getFundVersionId());
        vo.setState(PublicationStateInternal.fromValue(export.getState().name()));
        vo.setCreatedBy(createdBy);
        vo.setCreatedAt(export.getCreatedAt());
        vo.setPreparedAt(export.getPreparedAt());
        vo.setLastFetchedAt(export.getLastFetchedAt());
        vo.setPublishedAt(export.getPublishedAt());
        vo.setErrorAt(export.getErrorAt());
        vo.setInvalidatedAt(export.getInvalidatedAt());
        vo.setLastChangeId(export.getLastChangeId());
        vo.setErrorMessage(export.getErrorMessage());
        vo.setHasDownloadableFile(export.getFileId() != null);
        return vo;
    }

    private ArrExportType requireActiveType(final String code) {
        ArrExportType type = exportTypeRepository.findByCode(code)
                .orElseThrow(() -> new ObjectNotFoundException("Publication type not found", BaseCode.ID_NOT_EXIST).set("code", code));
        if (!Boolean.TRUE.equals(type.getActive())) {
            // Spec: inactive type → 403, internal state not exposed.
            throw new AccessDeniedException("Publication type is inactive").set("code", code);
        }
        return type;
    }

    private Long parseCursor(final String lastTransaction) {
        // Missing / empty cursor means "start from the beginning" — a legitimate
        // first call. Anything else must be a value we previously issued; if it
        // doesn't parse cleanly we treat it as a contract violation rather than
        // silently restarting (which could hide a publication-system bug).
        if (lastTransaction == null || lastTransaction.isBlank()) {
            return null;
        }
        long value;
        try {
            value = Long.parseLong(lastTransaction.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Malformed lastTransaction cursor",
                    ex, BaseCode.PROPERTY_IS_INVALID)
                    .set("lastTransaction", lastTransaction);
        }
        if (value < 0) {
            throw new BusinessException("Negative lastTransaction cursor",
                    BaseCode.PROPERTY_IS_INVALID)
                    .set("lastTransaction", lastTransaction);
        }
        return value;
    }

    private AvailablePublication toAvailableVO(final ArrExport e) {
        AvailablePublication vo = new AvailablePublication();
        vo.setPublicationId(e.getExportId());
        ArrFundVersion fv = e.getFundVersion();
        vo.setFundId(fv.getFund().getFundId());
        vo.setFundVersionId(fv.getFundVersionId());
        vo.setCreatedTime(e.getCreatedAt());
        vo.setLastChangeId(e.getLastChangeId());
        // publicationType identifies the XSD the publication system needs to
        // parse the XML — currently the only format Elza emits.
        vo.setPublicationType(XmlNameConsts.SCHEMA_URI);
        return vo;
    }

    private ArrFundVersion requireOpenVersion(final Integer fundId) {
        ArrFundVersion version = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if (version == null) {
            throw new ObjectNotFoundException("Open fund version not found",
                    BaseCode.ID_NOT_EXIST).set("fundId", fundId);
        }
        return version;
    }

    private ArrExportType requireActiveTypeById(final Integer typeId) {
        ArrExportType type = exportTypeRepository.findById(typeId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Publication type not found", BaseCode.ID_NOT_EXIST).setId(typeId));
        if (!Boolean.TRUE.equals(type.getActive())) {
            // Spec: inactive type → 403, internal state not exposed.
            throw new AccessDeniedException("Publication type is inactive").set("typeId", typeId);
        }
        return type;
    }

    private ArrExport requireExportInFund(final Integer fundId, final Integer publicationId) {
        ArrExport export = exportRepository.findById(publicationId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Publication not found", BaseCode.ID_NOT_EXIST).setId(publicationId));
        ArrFund fund = export.getFundVersion().getFund();
        if (!Objects.equals(fund.getFundId(), fundId)) {
            throw new ObjectNotFoundException("Publication does not belong to the requested fund",
                    BaseCode.ID_NOT_EXIST).setId(publicationId);
        }
        return export;
    }

    private UsrUser requireLoggedUser() {
        UsrUser user = userService.getLoggedUser();
        if (user == null) {
            throw new BusinessException("Publication requires an authenticated, non-admin user",
                    BaseCode.INSUFFICIENT_PERMISSIONS);
        }
        return user;
    }

    private String buildFileName(final ArrExport export) {
        ArrFund fund = export.getFundVersion().getFund();
        StringBuilder sb = new StringBuilder();
        boolean hasPart = false;

        if (fund.getMark() != null && !fund.getMark().isBlank()) {
            sb.append(fund.getMark());
            hasPart = true;
        }
        if (fund.getFundNumber() != null) {
            if (hasPart) {
                sb.append("-");
            }
            sb.append(fund.getFundNumber());
            hasPart = true;
        }
        if (!hasPart) {
            sb.append("fundId-").append(fund.getFundId());
        }
        sb.append("-").append(export.getExportId()).append(".xml");

        // mark is user-supplied and may contain characters invalid in filenames.
        return sb.toString().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /** Service-level wrapper that lets the controller add HTTP headers. */
    public static final class DownloadPayload {
        private final Resource resource;
        private final String fileName;

        public DownloadPayload(final Resource resource, final String fileName) {
            this.resource = resource;
            this.fileName = fileName;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * Pageable that honours an arbitrary offset (not just multiples of page
     * size) — Spring Data's {@code PageRequest} is page-index based.
     */
    private static final class RawOffsetPageable implements Pageable {

        private final long offset;
        private final int size;

        RawOffsetPageable(final long offset, final int size) {
            this.offset = offset;
            this.size = size;
        }

        @Override
        public int getPageNumber() {
            return 0;
        }

        @Override
        public int getPageSize() {
            return size;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public org.springframework.data.domain.Sort getSort() {
            return org.springframework.data.domain.Sort.unsorted();
        }

        @Override
        public Pageable next() {
            return new RawOffsetPageable(offset + size, size);
        }

        @Override
        public Pageable previousOrFirst() {
            return offset - size < 0 ? first() : new RawOffsetPageable(offset - size, size);
        }

        @Override
        public Pageable first() {
            return new RawOffsetPageable(0, size);
        }

        @Override
        public Pageable withPage(final int pageNumber) {
            return new RawOffsetPageable((long) pageNumber * size, size);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }
    }

}
