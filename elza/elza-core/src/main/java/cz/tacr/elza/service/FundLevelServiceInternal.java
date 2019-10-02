package cz.tacr.elza.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.repository.LevelRepository;

/**
 * Internal service for fund levels.
 *
 * No permissions are checked on any operation.
 */
@Service
public class FundLevelServiceInternal {

    private final LevelRepository levelRepository;

    @Autowired
    public FundLevelServiceInternal(LevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }

    /**
     * Test if specified change is last change for nodes. Parents and subtrees can be included.
     *
     * @param change tested change
     * @param nodeIds searched node ids
     * @param includeParents include parents up to root
     * @param includeChildren include children
     */
    public boolean isLastChange(final ArrChange change,
                                final int nodeId,
                                final boolean includeParents,
                                final boolean includeChildren) {
        Timestamp lastChange = Timestamp.valueOf(change.getChangeDate().toLocalDateTime());
        if (includeChildren) {
            List<Integer> newerNodeIds = levelRepository.findNewerNodeIdsInSubtree(nodeId, lastChange);
            if (newerNodeIds.size() > 0) {
                return false;
            }
        }
        if (includeParents) {
            List<Integer> newerNodeIds = levelRepository.findNewerNodeIdsInParents(nodeId, lastChange);
            if (newerNodeIds.size() > 0) {
                return false;
            }
        }
        return true;
    }
}
