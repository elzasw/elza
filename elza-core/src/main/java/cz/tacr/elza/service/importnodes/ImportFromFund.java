package cz.tacr.elza.service.importnodes;

import com.google.common.base.Objects;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.importnodes.vo.ChangeDeep;
import cz.tacr.elza.service.importnodes.vo.DeepCallback;
import cz.tacr.elza.service.importnodes.vo.File;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Node;
import cz.tacr.elza.service.importnodes.vo.Packet;
import cz.tacr.elza.service.importnodes.vo.Scope;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementace importu z jiného archivního souboru.
 *
 * @since 19.07.2017
 */
public class ImportFromFund implements ImportSource {

    // TODO: přepsat na prototype
    private final ScopeRepository scopeRepository;
    private final FundFileRepository fundFileRepository;
    private final PacketRepository packetRepository;
    private final LevelRepository levelRepository;

    private ArrFundVersion sourceFundVersion;
    private Set<Integer> nodeIds;
    private Iterator<Integer> nodeIdsIterator;
    private boolean ignoreRootNodes;

    private Integer nodeId = null;
    private LevelIterator levelsIterator;
    private ArrNode node;
    private ArrNode nodePrev;
    private Stack<ArrNode> nodeParents;

    public ImportFromFund(final ScopeRepository scopeRepository,
                          final FundFileRepository fundFileRepository,
                          final PacketRepository packetRepository,
                          final LevelRepository levelRepository) {
        this.scopeRepository = scopeRepository;
        this.fundFileRepository = fundFileRepository;
        this.packetRepository = packetRepository;
        this.levelRepository = levelRepository;
    }

    /**
     * Inicializace zdroje.
     *
     * @param sourceFundVersion zdrojová verze AS
     * @param sourceNodes       uzly, které prohledáváme
     * @param ignoreRootNodes   ignorují je vrcholové uzly, které prohledáváme
     */
    public void init(final ArrFundVersion sourceFundVersion, final Collection<ArrNode> sourceNodes, final boolean ignoreRootNodes) {
        this.sourceFundVersion = sourceFundVersion;
        this.nodeIds = sourceNodes.stream().map(ArrNode::getNodeId).collect(Collectors.toCollection(TreeSet::new));
        this.nodeIdsIterator = this.nodeIds.iterator();
        this.ignoreRootNodes = ignoreRootNodes;
    }

    @Override
    public Set<? extends Scope> getScopes() {
        return scopeRepository.findScopesBySubtreeNodeIds(nodeIds, ignoreRootNodes);
    }

    @Override
    public Set<? extends File> getFiles() {
        return fundFileRepository.findFileNamesBySubtreeNodeIds(nodeIds, ignoreRootNodes).stream().map(name -> (File) () -> name).collect(Collectors.toSet());
    }

    @Override
    public Set<? extends Packet> getPackets() {
        return packetRepository.findPacketsBySubtreeNodeIds(nodeIds, ignoreRootNodes);
    }

    @Override
    public boolean hasNext() {
        while (true) {
            if (nodeId == null) {
                if (nodeIdsIterator.hasNext()) {
                    nodeId = nodeIdsIterator.next();
                    levelsIterator = new LevelIterator(levelRepository, nodeId);
                    node = null;
                    nodePrev = null;
                    nodeParents = new Stack<>();
                } else {
                    break;
                }
            }
            if (levelsIterator.hasNext()) {
                return true;
            } else {
                nodeId = null;
            }
        }
        return false;
    }

    /**
     * Iterátor pro postupné získávání uzlů ve stromu/podstromu.
     *
     * Iterátor prochází strom do hloubky (DFS)!
     */
    public class LevelIterator implements Iterator<ArrLevel> {

        /**
         * Postup
         */
        private int offset = 0;

        /**
         * Maximální počet uzlů, které lze načíst na jediný dotaz z DB.
         */
        private final int MAX = 5; // TODO: zvýšit!!!!

        /**
         * Identifikátor uzlu od kterého se prohledává strom.
         */
        private final Integer nodeId;

        /**
         * Iterátor načtených uzlů.
         */
        private Iterator<ArrLevel> iterator = null;

        private final LevelRepository levelRepository;

        public LevelIterator(final LevelRepository levelRepository, final Integer nodeId) {
            this.levelRepository = levelRepository;
            this.nodeId = nodeId;
        }

        @Override
        public boolean hasNext() {
            if (iterator == null) { // pokud není nic načtené, načteme první část do bufferu
                iterator = levelRepository.findLevelsSubtree(nodeId, offset, MAX).iterator();
            }
            if (!iterator.hasNext()) { // pokud už nemáme v buffer, posuneme offset a načteme další část
                offset += MAX;
                iterator = levelRepository.findLevelsSubtree(nodeId, offset, MAX).iterator();
            }
            return iterator.hasNext();
        }

        @Override
        public ArrLevel next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            return iterator.next();
        }
    }

    @Override
    public Node getNext(final DeepCallback changeDeep) {
        if (!hasNext()) {
            throw new IllegalStateException();
        }

        ArrLevel level = levelsIterator.next();
        nodePrev = node;
        node = level.getNode();

        if ((nodePrev == null && nodeParents.empty())) {
            changeDeep.call(ChangeDeep.RESET);
        } else if (!nodeParents.empty() && Objects.equal(level.getNodeParent(), nodeParents.peek())) {
            changeDeep.call(ChangeDeep.NONE);
        } else if (level.getNodeParent() != null && level.getNodeParent().equals(nodePrev)) {
            nodeParents.push(level.getNodeParent());
            changeDeep.call(ChangeDeep.DOWN);
        } else {
            do {
                nodeParents.pop();
                changeDeep.call(ChangeDeep.UP);
            } while (!Objects.equal(level.getNodeParent(), nodeParents.peek()));
        }

        // TODO
        return () -> UUID.randomUUID().toString();
    }
}
