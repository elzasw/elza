package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;

import javax.annotation.Nullable;
import java.util.List;


/**
 * Rozšířené rozhraní repozitáře {@link LevelRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
public interface LevelRepositoryCustom {

    /**
     * Najde všechny rodiče seřazeny od listu ke kořenu podle nodu v požadované verzi stromu. Výsledek obsahuje i kořenový uzel verze.
     * @param node uzel
     * @param version verze stromu
     * @return všechny rodiče seřazeny od listu ke kořenu
     */
    List<ArrLevel> findAllParentsByNodeAndVersion(ArrNode node, ArrFundVersion version);


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
     * Najde všechny uzly s daným nodem.
     * @param node nod uzlů
     * @param change čas uzamčení uzamčení verze  (null pro otevřenou verzi)
     * @return seznam uzlů s daným nodem (sdílené uzly)
     */
    List<ArrLevel> findByNode(ArrNode node, @Nullable ArrChange change);

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
     * zjistí zda je level v zadané hierarchické struktuře.
     *
     * @param level    testovaný level.
     * @param rootNode kořen zadané hierarchické struktury.
     * @return true pokud je level v zadané hierarchické struktuře.
     */
    boolean isLevelInRootTree(ArrLevel level, ArrNode rootNode, @Nullable ArrChange lockChange);




    /**
     * Zjistí, jestli je daný node ve stejném stromu, jako je daný kořen. Pokud máme dva nody se stejným nodeId v
     * různých stromech, je potřeba najít tu entitu pro konkrétní strom.
     *
     * @param node     id nodu
     * @param rootNode id kořenu
     * @return nalezený level pro daný strom nebo null, pokud nebyl nalezen
     */
    ArrLevel findNodeInRootTreeByNodeId(ArrNode node, ArrNode rootNode, @Nullable ArrChange lockChange);

    /**
     * Vyhledá potomky, které mají vyšší datum poslední změny, než je v ArrChange.
     *
     * @param node   uzel od kterého prohledáváme
     * @param change změna podle které filtrujeme uzly
     * @return identifikátory uzlů, které byly změněny
     */
    List<Integer> findNodeIdsSubtree(ArrNode node, ArrChange change);

    /**
     * Vyhledá rodiče, které mají vyšší datum poslední změny, než je v ArrChange.
     *
     * @param node   uzel od kterého prohledáváme
     * @param change změna podle které filtrujeme uzly
     * @return identifikátory uzlů, které byly změněny
     */
    List<Integer> findNodeIdsParent(ArrNode node, ArrChange change);

    /**
     * Provede načtení všech uzlů ve stromu dané verze.
     *
     * @param version verze stromu
     * @return seznam všech uzlů ve stromu
     */
    List<LevelInfo> readTree(ArrFundVersion version);

    /**
     * @return vrací dodatečný text při použití rekurzivního dotazu SQL podle typu DB
     */
    String getRecursivePart();

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
