package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import jakarta.annotation.Nullable;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
public interface DescItemRepositoryCustom {

    /**
     * Provede načtení popisků uzlu pro seznam id uzlů.
     *
     * @param nodeIds    množina id uzlů, pro které chceme popisky
     * @param titleType  typ atributu, ze kterého bude načten popisek
     * @param lockChange změna uzavření verze
     * @return mapa popisků uzlů (nodeid -> popisek)
     */
    Map<Integer, DescItemTitleInfo> findDescItemTitleInfoByNodeId(Set<Integer> nodeIds,
                                                                  RulItemType titleType,
                                                                  @Nullable ArrChange lockChange);

    /**
     * Vyhledání hodnot atributů k požadovaným jednotkám popisu.
     *
     * @param nodeIds identifikátory jednotky popisu
     * @return mapa - klíč identifikátor jed. popisu, hodnota - seznam hodnot atributu
     */
    Map<Integer, List<ArrDescItem>> findByNodes(Collection<Integer> nodeIds);

    List<ArrDescItem> findDescItemsByNodeIds(Collection<Integer> nodeIds, Collection<RulItemType> itemTypes, Integer changeId);

    List<ArrDescItem> findByNodesContainingText(Collection<ArrNode> nodes,
                                                RulItemType itemType,
                                                Set<RulItemSpec> specifications,
                                                String text);

    List<ArrDescItem> findByNodesContainingStructureObjectIds(Collection<ArrNode> nodes,
                                                              RulItemType itemType,
                                                              Set<RulItemSpec> specifications,
                                                              Collection<Integer> stuctureObjectIds);


    /**
     * Objekt popisku uzlu.
     */
    class DescItemTitleInfo {

        /**
         * Nodeid uzlu.
         */
        private Integer nodeId;
        /**
         * Popisek uzlu.
         */
        private String value;

        private Integer nodeVersion;

        public DescItemTitleInfo() {
        }

        public DescItemTitleInfo(final Integer nodeId, final String value, final Integer nodeVersion) {
            this.nodeId = nodeId;
            this.value = value;
            this.nodeVersion = nodeVersion;
        }


        public Integer getNodeId() {
            return nodeId;
        }

        public void setNodeId(final Integer nodeId) {
            this.nodeId = nodeId;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public Integer getNodeVersion() {
            return nodeVersion;
        }

        public void setNodeVersion(final Integer nodeVersion) {
            this.nodeVersion = nodeVersion;
        }
    }
}
