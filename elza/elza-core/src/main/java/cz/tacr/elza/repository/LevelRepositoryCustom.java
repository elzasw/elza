package cz.tacr.elza.repository;

import java.sql.Timestamp;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;


/**
 * Rozšířené rozhraní repozitáře {@link LevelRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
public interface LevelRepositoryCustom {

    /**
     * Najde všechny rodiče od listu ke kořenu podle nodu v požadované verzi stromu. Výsledek obsahuje i kořenový uzel verze.
     * Result is always sorted, see description of {@code orderFromRoot} parameter.
     * @param node uzel
     * @param version verze stromu
     * @param orderFromRoot If true then result is sorted from fund root to parent of specified node otherwise order is reversed.
     */
    List<ArrLevel> findAllParentsByNodeId(final Integer nodeId, @Nullable ArrChange lockChange, boolean orderFromRoot);

    /**
     * Najde staršího sourozence daného uzlu.
     *
     * @param level      uzel, kterému hledáme sourozence
     * @param lockChange uzavření verze uzlu, ve které hledáme
     * @return nalezený sourozenece nebo null
     */
    ArrLevel findOlderSibling(ArrLevel level, @Nullable ArrChange lockChange);


    /**
     * Najde kompletní podstrom všech potomků.
     * @param node rodič
     * @param lockChange čas uzamčení uzamčení verze  (null pro otevřenou verzi)
     * @return kompletní podstrom všech potomků
     */
    List<ArrLevel> findAllChildrenByNode(ArrNode node, @Nullable ArrChange lockChange);


    /**
     * Najde všechny uzly daného rodiče.
     * @param nodeParent rodič
     * @param change čas uzamčení uzamčení verze  (null pro otevřenou verzi)
     * @return potomci rodiče
     */
    List<ArrLevel> findByParentNode(ArrNode nodeParent, @Nullable ArrChange change);


    /**
     * Najde uzel pro daný node.
     * @param node
     * @param change čas uzamčení uzamčení verze  (null pro otevřenou verzi)
     */
    ArrLevel findByNode(ArrNode node, @Nullable ArrChange change);

    /**
     * Najde uzel pro daný node.
     * @param nodeId
     * @param change čas uzamčení uzamčení verze  (null pro otevřenou verzi)
     */
     ArrLevel findByNodeId(final Integer nodeId, @Nullable final ArrChange change);

    /**
     * Vrací počet potomků daného uzlu.
     *
     * @param node       uzel
     * @param lockChange datum uzamčení uzlu
     * @return počet potomků daného uzlu
     */
    Integer countChildsByParent(ArrNode node, @Nullable ArrChange lockChange);


    /**
     * Najde všechny uzly v daném směru prohledávání.
     *
     * @param level     level, od kterého prohledáváme
     * @param version   verze, ve které prohledáváme
     * @param direction směr, kterým prohledáváme strom
     * @return všechny uzly v daném směru prohledávání
     */
    List<ArrLevel> findLevelsByDirection(ArrLevel level, ArrFundVersion version,
                                         RelatedNodeDirection direction);

    /**
     * Vyhledá potomky, které mají vyšší datum poslední změny. Specified node included.
     *
     * @param nodeId     uzel od kterého prohledáváme
     * @param lastUpdate změna podle které filtrujeme uzly
     * @return identifikátory uzlů, které byly změněny
     */
    List<Integer> findNewerNodeIdsInSubtree(int nodeId, Timestamp lastUpdate);

    /**
     * Vyhledá rodiče, které mají vyšší datum poslední změny. Specified node included.
     *
     * @param nodeId     uzel od kterého prohledáváme
     * @param lastUpdate změna podle které filtrujeme uzly
     * @return identifikátory uzlů, které byly změněny
     */
    List<Integer> findNewerNodeIdsInParents(int nodeId, Timestamp lastUpdate);

    /**
     * Vyhledání potomků v podstromu.
     *
     * Předpokladem je maximálně 1M záznamů v jedné úrovni!
     *
     * @param nodeId uzel prohledávání
     * @param skip   počet přeskočených záznamů
     * @param max    maximální počet vyhledaných záznamů
     * @param ignoreRootNode
     * @return seznam levelů
     */
    List<ArrLevel> findLevelsSubtree(Integer nodeId, final int skip, final int max, final boolean ignoreRootNode);

    /**
     * Iterate subtree BFS from specified node. Nodes in same depth are not ordered correctly,
     * but their position is fixed by sort of parent nodeId and level position.
     *
     * @param nodeId root node id of subtree
     * @param change when null open version is used
     * @param excludeRoot if true root node will be excluded
     * @param treeLevelConsumer action for each result
     */
    long readLevelTree(Integer nodeId, ArrChange change, boolean excludeRoot, TreeLevelConsumer treeLevelConsumer);

    /**
     * Provede načtení všech uzlů ve stromu/podstromu dané verze.
     *
     * @param change
     *            Poslední změna ve stromu, pokud je null tak načte poslední
     *            verziversion verze stromu
     * @param rootNodeId
     *            ID kořene stromu, který bude načten
     * @return seznam všech uzlů ve stromu
     */
    List<LevelInfo> readTree(final ArrChange change, final Integer rootNodeId);

    public interface TreeLevelConsumer {

        void accept(ArrLevel level, int depth);
    }

    /**
     * Uzel stromu, obsahuje pouze základní informace.
     */
    class LevelInfo {

        /**
         * NodeId uzlu.
         */
        private Integer nodeId;
        /**
         * Pozice uzlu.
         */
        private Integer position;
        /**
         * Nodeid rodiče uzlu.
         */
        private Integer parentId;

        /**
         * Nová instance.
         *
         * @param nodeId   NodeId uzlu.
         * @param position Pozice uzlu.
         * @param parentId Nodeid rodiče uzlu.
         */
        public LevelInfo(final Integer nodeId, final Integer position, final Integer parentId) {
            this.nodeId = nodeId;
            this.position = position;
            this.parentId = parentId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(final Integer position) {
            this.position = position;
        }

        public Integer getParentId() {
            return parentId;
        }

        public void setParentId(final Integer parentId) {
            this.parentId = parentId;
        }

    }
}
