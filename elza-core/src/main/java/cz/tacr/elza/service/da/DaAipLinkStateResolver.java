package cz.tacr.elza.service.da;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.api.AipLinkState;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;

/**
 * Owns how much of an AIP hangs on the archival description.
 *
 * A link may be made to the package as a whole, to a level of its logical structure or to one file,
 * and a link to something that contains files attaches those files too. How much of a package is
 * attached is therefore a question about what its links reach, not about which link rows exist, and
 * the content of a package is a graph rather than a list: every file sits under a representation
 * and, usually, under a level of the logical structure as well. Working that out for every row of a
 * listing is not affordable, so it is worked out whenever the links or the content change and kept
 * on the AIP.
 *
 * Like {@link DaAipReferenceResolver}, {@link #updateLinkState} only sets the value on the state it
 * is given and leaves storing it to the caller, which is what lets it be used inside the per-AIP
 * transactions the actions run in; {@link #refreshFor} is the convenience for callers that changed
 * the links and do not hold the state. The answer is always computed from scratch, so a call that
 * was missed somewhere is put right by the next one.
 */
@Service
public class DaAipLinkStateResolver {

    @Autowired
    private ArrDaLinkRepository daLinkRepository;

    @Autowired
    private AipStateRepository aipStateRepository;

    @Autowired
    private DaDaoRepository daoRepository;

    @Autowired
    private DaDaoRelationRepository daoRelationRepository;

    /**
     * Works out the link state of the AIP and sets it on the given state. The caller saves.
     */
    public void updateLinkState(DaAipState aipState) {
        aipState.setLinkState(computeLinkState(aipState.getDaAip()));
    }

    /**
     * Works out the link state of the AIP and stores it, for callers that changed its links and do
     * not hold its state.
     */
    @Transactional
    public void refreshFor(DaAip aip) {
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        if (aipState != null) {
            updateLinkState(aipState);
            aipStateRepository.save(aipState);
        }
    }

    public AipLinkState computeLinkState(DaAip aip) {
        List<ArrDaLink> liveLinks = daLinkRepository.findByAipIdAndDeleteChangeIsNull(aip.getAipId());
        if (liveLinks.isEmpty()) {
            return AipLinkState.NOT_LINKED;
        }
        // A link to the package as a whole attaches everything in it, whatever that turns out to be.
        if (liveLinks.stream().anyMatch(link -> link.getDaDao() == null)) {
            return AipLinkState.FULLY_LINKED;
        }

        Set<Integer> content = fileDaoIds(aip);
        if (content.isEmpty()) {
            // Nothing is known about what the package contains - its metadata has not been read yet.
            // "Everything is attached" would be true of an empty set and would say more than is known.
            return AipLinkState.PARTIALLY_LINKED;
        }

        Set<Integer> covered = filesReachableFrom(aip, linkedDaoIds(liveLinks), content);
        return covered.containsAll(content) ? AipLinkState.FULLY_LINKED : AipLinkState.PARTIALLY_LINKED;
    }

    /**
     * The archival content of the package. Only the files count: the metadata entities carry a file
     * of their own but describe the package rather than being what is arranged, and nothing ever
     * puts them under a parent, so no link could reach them anyway.
     */
    private Set<Integer> fileDaoIds(DaAip aip) {
        Set<Integer> fileDaoIds = new HashSet<>();
        for (DaDao dao : daoRepository.findByAipAndTypeAndDeleteChangeIsNull(aip, DaDao.DaoType.FILE)) {
            fileDaoIds.add(dao.getDaoId());
        }
        return fileDaoIds;
    }

    private static Set<Integer> linkedDaoIds(List<ArrDaLink> liveLinks) {
        Set<Integer> linked = new HashSet<>();
        for (ArrDaLink link : liveLinks) {
            if (link.getDaDao() != null) {
                linked.add(link.getDaDao().getDaoId());
            }
        }
        return linked;
    }

    /**
     * The files reached by walking down from the attached entities.
     *
     * The whole graph of the AIP is read at once and walked in memory: a package holds tens of
     * entities, and doing it here keeps the traversal out of SQL, which would have to be written
     * once per database. A file is reached through any of its parents - both the representation it
     * belongs to and the level of the logical structure it hangs under attach it - which is also how
     * the package browser presents it.
     */
    private Set<Integer> filesReachableFrom(DaAip aip, Set<Integer> linkedDaoIds, Set<Integer> content) {
        Map<Integer, List<Integer>> childrenByParent = new HashMap<>();
        for (DaDaoRelation relation : daoRelationRepository.findByAipsAndDeleteChangeIsNull(List.of(aip))) {
            childrenByParent.computeIfAbsent(relation.getParentDao().getDaoId(), id -> new ArrayList<>())
                    .add(relation.getDao().getDaoId());
        }

        Set<Integer> covered = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>(linkedDaoIds);
        while (!queue.isEmpty()) {
            Integer daoId = queue.poll();
            // The same entity is reachable by more than one path, and nothing stops a package from
            // declaring the same relation twice.
            if (!visited.add(daoId)) {
                continue;
            }
            if (content.contains(daoId)) {
                covered.add(daoId);
            }
            queue.addAll(childrenByParent.getOrDefault(daoId, List.of()));
        }
        return covered;
    }
}
