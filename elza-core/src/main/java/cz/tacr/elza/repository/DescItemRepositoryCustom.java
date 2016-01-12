package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
public interface DescItemRepositoryCustom {

    List<ArrDescItem> findByNodes(Collection<ArrNode> nodes, @Nullable ArrChange lockChange);


    /**
     * Provede načtení popisků uzlu pro seznam id uzlů.
     *
     * @param nodeIds    množina id uzlů, pro které chceme popisky
     * @param titleType  typ atributu, ze kterého bude načten popisek
     * @param lockChange změna uzavření verze
     * @return mapa popisků uzlů (nodeid -> popisek)
     */
    Map<Integer, DescItemTitleInfo> findDescItemTitleInfoByNodeId(Set<Integer> nodeIds,
                                                                  RulDescItemType titleType,
                                                                  @Nullable ArrChange lockChange);


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

        public DescItemTitleInfo() {
        }

        public DescItemTitleInfo(final Integer nodeId, final String value) {
            this.nodeId = nodeId;
            this.value = value;
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
    }
}
