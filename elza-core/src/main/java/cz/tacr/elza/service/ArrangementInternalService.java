package cz.tacr.elza.service;

import com.google.common.collect.Iterables;
import cz.tacr.elza.core.db.HibernateConfiguration;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserDetail;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal arrangement service.
 */
@Service
public class ArrangementInternalService {

    private static final AtomicInteger LAST_DESC_ITEM_OBJECT_ID = new AtomicInteger(-1);

    @Autowired
    private ChangeRepository changeRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManager em;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DescItemRepository descItemRepository;

    /**
     * Vytvoření objektu pro změny s primárním uzlem.
     *
     * @param type        typ změny
     * @param primaryNode primární uzel
     * @return objekt změny
     */
    public ArrChange createChange(@Nullable final ArrChange.Type type,
                                  @Nullable final ArrNode primaryNode) {
        ArrChange change = new ArrChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(OffsetDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setPrimaryNode(primaryNode);

        return changeRepository.save(change);
    }

    /**
     * Vytvoření objektu pro změny.
     *
     * @param type typ změny
     * @return objekt změny
     */
    public ArrChange createChange(@Nullable final ArrChange.Type type) {
        return createChange(type, null);
    }

    /**
     * Zjistí, jestli patří vybraný level do dané verze.
     *
     * @param level   level
     * @param version verze
     * @return true pokud patří uzel do verze, jinak false
     */
    public boolean validLevelInVersion(final ArrLevel level, final ArrFundVersion version) {
        Assert.notNull(level, "Musí být vyplněno");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Integer lockChange = version.getLockChange() == null
                ? Integer.MAX_VALUE : version.getLockChange().getChangeId();

        int levelDeleteChange = level.getDeleteChange() == null ?
                Integer.MAX_VALUE : level.getDeleteChange().getChangeId();

        return level.getCreateChange().getChangeId() < lockChange && levelDeleteChange >= lockChange;
    }

    /**
     * Načte neuzavřené verze archivních pomůcek.
     *
     * @param fundIds ids archivních pomůcek
     * @return verze
     */
    public List<ArrFundVersion> getOpenVersionsByFundIds(final Collection<Integer> fundIds) {
        Assert.notNull(fundIds, "Nebyl vyplněn identifikátor AS");
        if (fundIds.isEmpty()) {
            return Collections.emptyList();
        }
        return fundVersionRepository.findByFundIdsAndLockChangeIsNull(fundIds);
    }

    public List<ArrNode> findNodesByStructuredObjectId(Integer structuredObjectId) {
        return nodeRepository.findNodesByStructuredObjectIds(Collections.singletonList(structuredObjectId));
    }

    public Map<Integer, ArrNode> findNodesByStructuredObjectIds(Collection<Integer> structuredObjectIds) {
        if (structuredObjectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ArrNode> result = new HashMap<>(HibernateConfiguration.MAX_IN_SIZE);
        for (List<Integer> idsPart : Iterables.partition(structuredObjectIds, HibernateConfiguration.MAX_IN_SIZE)) {
            for (ArrNode node : nodeRepository.findNodesByStructuredObjectIds(idsPart)) {
                result.put(node.getNodeId(), node);
            }
        }
        return result;
    }

    public Integer getNextDescItemObjectId() {
        return LAST_DESC_ITEM_OBJECT_ID.updateAndGet(id -> {
            if (id < 0) {
                id = itemRepository.findMaxItemObjectId();
            }
            return id + 1;
        });
    }

    /**
     * Migrace typu objektu změny.
     *
     * @param change  migrovaná změna
     * @param newType nový typ změny
     * @return upravený objekt změny
     */
    public ArrChange migrateChangeType(final ArrChange change, final ArrChange.Type newType) {
        Validate.notNull(change);
        Validate.notNull(newType);
        Validate.notNull(change.getChangeId());
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(OffsetDateTime.now());
        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }
        change.setType(newType);
        return changeRepository.save(change);
    }

    /**
     * Count active items linked to the file
     * 
     * @param file
     * @return
     */
    public Integer countActiveItems(ArrFile file) {
        return descItemRepository.countItemsUsingFile(file);
    }
}
