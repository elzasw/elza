package cz.tacr.elza.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nullable;

import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserDetail;

/**
 * Internal arrangement service.
 */
@Service
public class ArrangementInternalService {

    private static final AtomicInteger LAST_DESC_ITEM_OBJECT_ID = new AtomicInteger(-1);

    final private static Logger logger = LoggerFactory.getLogger(ArrangementInternalService.class);

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
     * Find node by UUID
     *
     * @param nodeUuid
     * @return
     */
    public ArrNode findNodeByUuid(final String nodeUuid) {
        return nodeRepository.findOneByUuid(nodeUuid);
    }

    /**
     * Zjistí, jestli patří vybraný level do dané verze.
     *
     * @param level   level
     * @param version verze
     */
    public void checkLevelInVersion(final ArrLevel level, final ArrFundVersion version) {
        Validate.notNull(level, "Musí být vyplněno");
        Validate.notNull(version, "Verze AS musí být vyplněna");

        //
        if (level.getDeleteChange() != null) {
            // Kontrola, zda nedoslo k zaniku urovne pred vznikem verze
            if (version.getCreateChange().getChangeDate().isAfter(level.getDeleteChange().getChangeDate())) {
                throw new SystemException("Level s id " + level.getLevelId() + " nespadá do verze s id " + version
                        .getFundVersionId());
            }
        }

        if (version.getLockChange() != null) {
            // kontrola, zda uzel nevznikl az po ukonceni verze
            if (level.getCreateChange().getChangeDate().isAfter(version.getLockChange().getChangeDate())) {
                throw new SystemException("Level s id " + level.getLevelId() + " nespadá do verze s id " + version
                        .getFundVersionId());
            }
        }
    }

    /**
     * Načte neuzavřené verze archivních pomůcek.
     *
     * @param fundIds ids archivních pomůcek
     * @return verze
     */
    public List<ArrFundVersion> getOpenVersionsByFundIds(final Collection<Integer> fundIds) {
        Validate.notNull(fundIds, "Nebyl vyplněn identifikátor AS");
        if (fundIds.isEmpty()) {
            return Collections.emptyList();
        }
        return fundVersionRepository.findByFundIdsAndLockChangeIsNull(fundIds);
    }

    /**
     * Načte neuzavřenou verzi AS
     *
     * @param fund AS
     * @return verze
     */
    public ArrFundVersion getOpenVersionByFund(final ArrFund fund) {
    	Validate.notNull(fund, "Nebyl vyplněn AS");
        return getOpenVersionByFundId(fund.getFundId());
    }

    /**
     * Načte neuzavřenou verzi AS
     *
     * Součástí je načtení i ArrFund
     *
     * @param fundId
     *            id AS
     * @return Aktuální verze
     */
    public ArrFundVersion getOpenVersionByFundId(final Integer fundId) {
    	Validate.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        if(fundVersion==null) {
        	throw new SystemException("Cannot find open version", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("fundId", fundId);
        }
        return fundVersion;
    }

    /**
     * Try to find fund by string
     *
     * Method search fund by UUID, internalCode and fundId
     *
     * @param fundIdentifier
     * @return fund, throw exception if not found
     */
    public ArrFundVersion getOpenVersionByString(String fundIdentifier) {
        logger.debug("Looking for fund: {}", fundIdentifier);
        // try to find by uuid
        if (fundIdentifier.length() == 36) {
            ArrFundVersion fv = fundVersionRepository.findByRootNodeUuid(fundIdentifier);
            if (fv != null) {
                logger.debug("Found by UUID as {}", fv.getFundId());
                return fv;
            }
        }
        // try to find by internal code
        ArrFundVersion fv = fundVersionRepository.findByInternalCode(fundIdentifier);
        if (fv != null) {
            logger.debug("Found by internal code as {}", fv.getFundId());
            return fv;
        }

        // try to find by id
        try {
            Integer id = Integer.valueOf(fundIdentifier);
            return getOpenVersionByFundId(id);
        } catch (NumberFormatException nfe) {
            throw new ObjectNotFoundException("Nebyl nalezen AS s ID=" + fundIdentifier,
                    ArrangementCode.FUND_NOT_FOUND)
                    .setId(fundIdentifier);
        }
    }

    public ArrFundVersion getFundVersionById(final Integer fundVersionId) {
        return fundVersionRepository.getOneCheckExist(fundVersionId);
    }

    public List<ArrNode> findNodesByStructuredObjectId(Integer structuredObjectId) {
        return nodeRepository.findNodesByStructuredObjectIds(Collections.singletonList(structuredObjectId));
    }

    public Map<Integer, ArrNode> findNodesByStructuredObjectIds(Collection<Integer> structuredObjectIds) {
        if (structuredObjectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ArrNode> result = new HashMap<>(ObjectListIterator.getMaxBatchSize());
        for (List<Integer> idsPart : Iterables.partition(structuredObjectIds, ObjectListIterator.getMaxBatchSize())) {
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

    /**
     * Check if level is deleted in given version
     * @param level
     * @param version
     * @return
     */
    public boolean isLevelDeletedInVersion(ArrLevel level, ArrFundVersion version) {
        // if not deleted -> simply return as not deleted
        if(level.getDeleteChange()==null) {
            return false;
        }
        if(version.getLockChange()!=null) {
            // closed version, check if not deleted later
            if (level.getDeleteChange().getChangeDate().isAfter(version.getLockChange().getChangeDate())) {
                return false;
            }
        }
        return true;
    }
}
