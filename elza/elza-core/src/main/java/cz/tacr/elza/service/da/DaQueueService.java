package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.controller.vo.AipType;
import cz.tacr.elza.controller.vo.DaQueueActionResult;
import cz.tacr.elza.controller.vo.DaQueueDirection;
import cz.tacr.elza.controller.vo.DaQueueItemPage;
import cz.tacr.elza.controller.vo.DaQueueItemVO;
import cz.tacr.elza.controller.vo.QueueItemState;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * The queue of a digital archive as the administrator sees and steers it.
 *
 * The queue keeps every request - waiting, finished, replaced or cancelled - so the list is
 * paged on the server and opens with the waiting ones. Nothing is deleted: a stored package
 * points to the request that downloaded it.
 */
@Service
public class DaQueueService {

    private static final Set<DaSyncQueueItem.QueueItemState> WAITING = EnumSet.of(
            DaSyncQueueItem.QueueItemState.IMPORT_NEW,
            DaSyncQueueItem.QueueItemState.UPDATE,
            DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED,
            DaSyncQueueItem.QueueItemState.EXPORT_NEW,
            DaSyncQueueItem.QueueItemState.EXPORT_SENT);

    private static final Set<DaSyncQueueItem.QueueItemState> EXPORT = EnumSet.of(
            DaSyncQueueItem.QueueItemState.EXPORT_NEW,
            DaSyncQueueItem.QueueItemState.EXPORT_SENT,
            DaSyncQueueItem.QueueItemState.EXPORT_OK,
            DaSyncQueueItem.QueueItemState.EXPORT_ERROR);

    private static final Set<DaSyncQueueItem.QueueItemState> ERROR = EnumSet.of(
            DaSyncQueueItem.QueueItemState.IMPORT_ERROR,
            DaSyncQueueItem.QueueItemState.EXPORT_ERROR);

    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private DaSyncQueueItemRepository queueRepository;
    @Autowired
    private DaService daService;
    @Autowired
    private ExternalSystemService externalSystemService;

    /** What the list is filtered by; null and empty values do not filter. */
    public record Filter(boolean all, @Nullable Collection<QueueItemState> states,
                         @Nullable DaQueueDirection direction, @Nullable String aipCode,
                         @Nullable String batchId, boolean failedOnly) {
    }

    /**
     * Requests of the repository, newest first.
     *
     * @param from  offset of the first request; taken as a page of {@code count}
     */
    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    @Transactional(readOnly = true)
    public DaQueueItemPage find(Integer repositoryId, Filter filter, int from, int count) {
        ArrDigitalRepository repository = repository(repositoryId);
        int size = Math.max(1, Math.min(count, MAX_PAGE_SIZE));
        Page<DaSyncQueueItem> page = queueRepository.findAll(specification(repository, filter),
                PageRequest.of(Math.max(from, 0) / size, size, Sort.by(Sort.Direction.DESC, "syncQueueItemId")));
        DaQueueItemPage result = new DaQueueItemPage();
        result.setItems(page.getContent().stream().map(DaQueueService::toVO).toList());
        result.setTotalCount((int) page.getTotalElements());
        return result;
    }

    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    @Transactional
    public DaQueueActionResult retryNow(Integer repositoryId, Collection<Integer> itemIds) {
        return apply(repositoryId, itemIds, DaService::isWaiting, items -> {
            daService.retryNow(items);
            return items.size();
        });
    }

    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    @Transactional
    public DaQueueActionResult withdraw(Integer repositoryId, Collection<Integer> itemIds) {
        return apply(repositoryId, itemIds, DaService::isUndelivered, items -> {
            daService.withdraw(items);
            return items.size();
        });
    }

    @AuthMethod(permission = UsrPermission.Permission.ADMIN)
    @Transactional
    public DaQueueActionResult repeat(Integer repositoryId, Collection<Integer> itemIds) {
        return apply(repositoryId, itemIds, DaService::isFailed, daService::repeat);
    }

    /**
     * Carries the action out on the selected requests of the repository it applies to; the rest
     * are counted as skipped - the list the selection was made in may be older than the queue.
     */
    private DaQueueActionResult apply(Integer repositoryId, Collection<Integer> itemIds,
                                      Predicate<DaSyncQueueItem> applies,
                                      java.util.function.Function<List<DaSyncQueueItem>, Integer> action) {
        ArrDigitalRepository repository = repository(repositoryId);
        List<DaSyncQueueItem> applicable = new ArrayList<>();
        for (DaSyncQueueItem item : queueRepository.findAllById(itemIds)) {
            if (item.getDigitalRepository().getExternalSystemId().equals(repository.getExternalSystemId())
                    && applies.test(item)) {
                applicable.add(item);
            }
        }
        int done = applicable.isEmpty() ? 0 : action.apply(applicable);
        DaQueueActionResult result = new DaQueueActionResult();
        result.setDone(done);
        result.setSkipped(Math.max(itemIds.size() - done, 0));
        return result;
    }

    private ArrDigitalRepository repository(Integer repositoryId) {
        SysExternalSystem system = externalSystemService.findExternalSystemById(repositoryId);
        if (!(system instanceof ArrDigitalRepository repository)) {
            throw new ObjectNotFoundException("Digitální repozitář neexistuje: " + repositoryId, BaseCode.ID_NOT_EXIST)
                    .setId(repositoryId);
        }
        return repository;
    }

    private static Specification<DaSyncQueueItem> specification(ArrDigitalRepository repository, Filter filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("digitalRepository"), repository));
            if (!filter.all()) {
                where.add(cb.isTrue(root.get("active")));
                where.add(root.get("state").in(WAITING));
            }
            if (filter.states() != null && !filter.states().isEmpty()) {
                where.add(root.get("state").in(filter.states().stream()
                        .map(state -> DaSyncQueueItem.QueueItemState.valueOf(state.name())).toList()));
            }
            if (filter.direction() != null) {
                where.add(filter.direction() == DaQueueDirection.EXPORT
                        ? root.get("state").in(EXPORT)
                        : cb.not(root.get("state").in(EXPORT)));
            }
            if (StringUtils.isNotBlank(filter.aipCode())) {
                where.add(cb.like(cb.lower(root.get("code")), "%" + filter.aipCode().trim().toLowerCase() + "%"));
            }
            if (StringUtils.isNotBlank(filter.batchId())) {
                where.add(cb.equal(root.get("batchId"), filter.batchId().trim()));
            }
            if (filter.failedOnly()) {
                where.add(cb.or(cb.greaterThan(root.get("attemptCount"), 0), root.get("state").in(ERROR)));
            }
            return cb.and(where.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    static DaQueueItemVO toVO(DaSyncQueueItem item) {
        DaQueueItemVO vo = new DaQueueItemVO();
        vo.setId(item.getSyncQueueItemId());
        vo.setAipId(item.getAip() == null ? null : item.getAip().getAipId());
        vo.setAipCode(item.getCode());
        vo.setAipVersion(item.getAipVersion());
        vo.setDirection(EXPORT.contains(item.getState()) ? DaQueueDirection.EXPORT : DaQueueDirection.DOWNLOAD);
        vo.setAipType(item.getAipType() == null ? null : AipType.valueOf(item.getAipType().name()));
        vo.setState(QueueItemState.valueOf(item.getState().name()));
        vo.setActive(Boolean.TRUE.equals(item.getActive()));
        vo.setBatchId(item.getBatchId());
        vo.setAttemptCount(item.getAttemptCount() == null ? 0 : item.getAttemptCount());
        vo.setNextAttemptAt(item.getNextAttemptAt());
        vo.setStateDate(item.getDate());
        vo.setStateMessage(item.getStateMessage());
        DaAipAction action = item.getAipActionItem() == null ? null : item.getAipActionItem().getAipAction();
        UsrUser user = action == null ? null : action.getUser();
        vo.setRequestedBy(user == null ? null : user.getUsername());
        return vo;
    }
}
