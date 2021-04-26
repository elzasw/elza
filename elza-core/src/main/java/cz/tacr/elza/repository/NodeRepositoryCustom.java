package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.ScrollableResults;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.filter.DescItemTypeFilter;


/**
 * Rozšíření repozitáře {@link NodeRepository}.
 *
 */
public interface NodeRepositoryCustom {

    public static class ArrDescItemInfo {

        private final Integer itemId;
        private final Integer nodeId;
        private final Integer fundId;
        private final Float score;

        public Integer getItemId() {
            return itemId;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public Integer getFundId() {
            return fundId;
        }

        public Float getScore() {
            return score;
        }

        public ArrDescItemInfo(final Integer itemId,
                               final Integer nodeId,
                               final Integer fundId,
                               final Float score) {
            this.itemId = itemId;
            this.nodeId = nodeId;
            this.fundId = fundId;
            this.score = score;
        }
    }

    /**
     * Najde všechny uzly v daném směru prohledávání.
     *
     * @param node      uzel, od kterého prohledáváme
     * @param version   verze, ve které prohledáváme
     * @param direction směr, kterým prohledáváme strom
     * @return všechny uzly v daném směru prohledávání
     */
    List<ArrNode> findNodesByDirection(ArrNode node, ArrFundVersion version,
                                       RelatedNodeDirection direction);

    /**
     * Najde uzly s danou hodnotou.
     *
     * @param fundId id fondů, do kterých uzly patří
     * @param text hledaná hodnota
     * @return množina id uzlů odopovídající hledané hodnotě
     */
    //List<ArrFundToNodeList> findFundIdsByFulltext(String text, Collection<ArrFund> fundList);

    /**
     * Najde uzly s danou hodnotou.
     *
     * @param fundId id fondů, do kterých uzly patří
     * @param text hledaná hodnota
     * @return množina id uzlů odopovídající hledané hodnotě
     */
    QueryResults<ArrDescItemInfo> findFundIdsByFulltext(String text, Collection<ArrFund> fundList,
                                                  Integer size,
                                                  Integer offset);
    /**
     * Najde uzly s danou hodnotou.
     *
     * @param text hledaná hodnota
     * @param fundId id fondu do kterého uzly patří
     * @param lockChangeId id verze ve které se má hledat, může být null
     *
     * @return množina id uzlů odopovídající hledané hodnotě
     */
    Set<Integer> findByFulltextAndVersionLockChangeId(String text, Integer fundId, Integer lockChangeId);


    /**
     * Najde uzly s danou hodnotou podle lucene dotazu.
     *
     * @param queryText např: +specification:*čís* -fulltextValue:ddd
     * @param fundId id fondu do kterého uzly patří
     * @param lockChangeId id verze ve které se má hledat, může být null
     *
     * @return množina id uzlů odopovídající hledané hodnotě
     * @throws InvalidQueryException neplatný lucene dotaz
     */
    Set<Integer> findByLuceneQueryAndVersionLockChangeId(String queryText, Integer fundId, Integer lockChangeId)
        throws InvalidQueryException;


    List<ArrNode> findByNodeConformityIsNull();

    /**
     * Najde id nodů v dané verzi odpovídající filtrům. Pokud nejsou filtry předány vrátí se id všech nodů ve verzi.
     *
     * @param version verze
     * @param descItemFilters filtry
     *
     * @return id nodů odpovídající parametrům
     */
    Set<Integer> findNodeIdsByFilters(ArrFundVersion version, List<DescItemTypeFilter> descItemFilters);

    /**
     * Vyhledání id nodů podle parametrů.
     *
     * @param searchParams parametry pro rozšířené vyhledávání
     * @param fundId id fondu do kterého uzly patří
     * @param lockChangeId id verze ve které se má hledat, může být null
     *
     * @return množina id uzlů které vyhovují parametrům
     */
    Set<Integer> findBySearchParamsAndVersionLockChangeId(List<SearchParam> searchParams, Integer fundId,
            Integer lockChangeId);

	/**
	 * Return list of uncached nodes
	 *
	 * @return
	 */
	ScrollableResults findUncachedNodes();
}
