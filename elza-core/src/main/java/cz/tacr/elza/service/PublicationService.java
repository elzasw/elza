package cz.tacr.elza.service;

import java.time.OffsetDateTime;
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
import cz.tacr.elza.domain.ArrExport;
import cz.tacr.elza.domain.ArrExport.State;
import cz.tacr.elza.domain.ArrExportType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DmsFile;
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
        // PREPARED means no download has happened yet — a status report at this point is a protocol violation.
        if (state == State.PREPARED) {
            throw new ConflictException(
                    "Publication has not been fetched yet", BaseCode.INVALID_STATE)
                    .set("id", publicationId).set("state", state.name());
        }

        PublicationReportStatus reported = report.getStatus();
        OffsetDateTime reportedAt = report.getPublishedAt();

        if (reported == PublicationReportStatus.OK) {
            if (state == State.PUBLISHED) {
                // Idempotent replay of an already-accepted OK report.
                if (Objects.equals(export.getPublishedAt(), reportedAt)) {
                    return;
                }
                // OK with a different publishedAt is not allowed to overwrite a successful publication.
                throw new ConflictException(
                        "Publication already reported with a different publishedAt",
                        BaseCode.INVALID_STATE)
                        .set("id", publicationId)
                        .set("storedPublishedAt", export.getPublishedAt())
                        .set("reportedPublishedAt", reportedAt);
            }
            // FETCHED → PUBLISHED, or recovery PUBLISH_ERROR → PUBLISHED.
            export.setState(State.PUBLISHED);
            export.setPublishedAt(reportedAt);
            export.setErrorMessage(null);
            export.setErrorAt(null);
        } else { // ERROR
            if (state == State.PUBLISHED) {
                // Don't let a late ERROR roll back an already-successful publication.
                throw new ConflictException(
                        "Publication is already PUBLISHED, cannot report ERROR",
                        BaseCode.INVALID_STATE).set("id", publicationId);
            }
            // Idempotent replay of an already-accepted ERROR report.
            if (state == State.PUBLISH_ERROR
                    && Objects.equals(export.getErrorAt(), reportedAt)
                    && Objects.equals(export.getErrorMessage(), report.getErrorMessage())) {
                return;
            }
            // FETCHED → PUBLISH_ERROR, or refresh of error details on PUBLISH_ERROR.
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
        requireOpenVersion(fundId);

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
        ArrExportType type = requireType(publicationTypeId);

        if (exportRepository.countOutstanding(fundVersion.getFundVersionId(), publicationTypeId) > 0) {
            throw new ConflictException("Pending or prepared publication already exists for this fund and type", BaseCode.DB_INTEGRITY_PROBLEM)
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
        if (file != null) {
            dmsService.deleteFile(file);
        }
    }

    @Transactional
    public PublicationDetail copy(final Integer fundId,
                                  final Integer publicationId,
                                  final Integer targetPublicationTypeId) {
    	ArrFundVersion fundVersion = requireOpenVersion(fundId);
        ArrExport source = requireExportInFund(fundId, publicationId);
        ArrExportType targetType = requireType(targetPublicationTypeId);

        Integer sourceFilterId = source.getExportType().getExportFilterId();
        Integer targetFilterId = targetType.getExportFilterId();
        if (!Objects.equals(sourceFilterId, targetFilterId)) {
            throw new ConflictException(
                    "Source and target publication types use incompatible filter settings",
                    BaseCode.INVALID_STATE)
                    .set("sourceTypeId", source.getExportType().getExportTypeId())
                    .set("targetTypeId", targetPublicationTypeId);
        }

        if (exportRepository.countOutstanding(source.getFundVersionId(), targetPublicationTypeId) > 0) {
            throw new ConflictException(
                    "Pending or prepared publication already exists for the target type",
                    BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("fundVersionId", source.getFundVersionId())
                    .set("publicationTypeId", targetPublicationTypeId);
        }

        ArrExport copy = new ArrExport();
        copy.setExportType(targetType);
        copy.setFundVersion(source.getFundVersion());
        copy.setExportFilter(targetType.getExportFilter());
        copy.setState(ArrExport.State.NEW);
        copy.setCreatedAt(OffsetDateTime.now());
        copy.setUser(requireLoggedUser());
        copy = exportRepository.save(copy);

        asyncRequestService.enqueue(fundVersion, copy, copy.getUserId());        

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
        if (lastTransaction == null || lastTransaction.isBlank()) {
            return null;
        }
        try {
            long v = Long.parseLong(lastTransaction.trim());
            return v < 0 ? null : v;
        } catch (NumberFormatException ex) {
            // Unknown / corrupt cursors are treated as "start over" rather than 400 —
            // a publication system that lost its state can resume cleanly.
            return null;
        }
    }

    private AvailablePublication toAvailableVO(final ArrExport e) {
        AvailablePublication vo = new AvailablePublication();
        vo.setPublicationId(e.getExportId());
        ArrFundVersion fv = e.getFundVersion();
        vo.setFundId(fv.getFund().getFundId());
        vo.setFundVersionId(fv.getFundVersionId());
        vo.setCreatedTime(e.getCreatedAt());
        vo.setLastChangeId(e.getLastChangeId());
        // TODO: publicationType is the XSD namespace URI. Wire it from the export
        //       filter / type once that mapping is finalised; "v2" is the only
        //       value we emit today.
        vo.setPublicationType("http://elza.tacr.cz/schema/v2");
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

    private ArrExportType requireType(final Integer typeId) {
        return exportTypeRepository.findById(typeId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Publication type not found", BaseCode.ID_NOT_EXIST).setId(typeId));
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
        String prefix = fund.getMark();
        if (prefix == null || prefix.isBlank()) {
            Integer number = fund.getFundNumber();
            if (number != null) {
                prefix = number.toString();
            }
        }
        if (prefix == null || prefix.isBlank()) {
            return "fundId-" + fund.getFundId() + "-" + export.getExportId() + ".xml";
        }
        Integer number = fund.getFundNumber();
        return prefix + "-" + (number == null ? "" : number) + "-" + export.getExportId() + ".xml";
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
