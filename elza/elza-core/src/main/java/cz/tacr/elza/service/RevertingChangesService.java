package cz.tacr.elza.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.view.ViewTitles;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundStructureExtension;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.ArrRequestQueueItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.ArrFileRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LockedValueRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.events.EventFunds;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventStructureDataChange;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.Change;
import cz.tacr.elza.service.vo.ChangesResult;
import cz.tacr.elza.service.vo.TitleItemsByType;

/**
 * Servisní třída pro práci s obnovou změn v archivní souboru - "UNDO".
 *
 */
@Service
public class RevertingChangesService {

    static private final Logger logger = LoggerFactory.getLogger(RevertingChangesService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private ConfigView configView;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private LockedValueRepository usedValueRepository;

    @Autowired
    private StructuredItemRepository structuredItemRepository;

    @Autowired
    private StructuredObjectRepository structuredObjectRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private StructObjService structObjService;

    @Autowired
    private StructObjValueService structObjValueService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private ArrFileRepository arrFileRepository;

    @Autowired
    private DmsService dmsService;
    
    @Autowired
    private StaticDataService staticDataService;

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS.
     *
     * @param fundVersion verze AS nad kterou provádím vyhledávání
     * @param node        JP omezující vyhledávání
     * @param maxSize     maximální počet záznamů
     * @param offset      počet přeskočených záznamů
     * @param fromChange  změna, vůči které chceme počítat offset (pokud není vyplněn, bere se vždy poslední)
     * @return výsledek hledání
     */
    public ChangesResult findChanges(@NotNull final ArrFundVersion fundVersion,
                                     @Nullable final ArrNode node,
                                     final int maxSize,
                                     final int offset,
                                     @Nullable final ArrChange fromChange) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");

        Integer fundId = fundVersion.getFund().getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange == null ? null : fromChange.getChangeId();
        boolean isNodeContext = node != null;

        // dotaz pro zjištění poslední změny (pro nastavení parametru outdated)
        ChangeResult lastChange = getLastChange(fundId, nodeId);
        if (lastChange == null) {
            throw new BusinessException("Failed to find valid last change", ArrangementCode.DATA_NOT_FOUND)
                    .set("nodeId", nodeId);
        }

        // dotaz pro celkový počet položek
        int count = countChange(fundId, nodeId, fromChangeId);

        // nalezené změny
        List<ChangeResult> changeList = findChange(fundId, nodeId, maxSize, offset, fromChangeId);

        // typ oprávnění, podle kterého se určuje, zda-li je možné provést rozsáhlejší revert, nebo pouze své změny
        boolean fullRevertPermission = hasFullRevertPermission(fundVersion.getFund());

        // kontrola, že neexistuje předchozí změna od jiného uživatele (true - neexistuje a můžu provést revert)
        boolean canRevertByUserBefore = true;

        // pokud nemám vyšší oprávnění a načítám starší změny, kontroluji, že neexistuje předchozí změna jiného uživatele
        if (!fullRevertPermission && offset > 0) {
            int otherUserChangeCount = countUserChange(fundId, nodeId, offset, fromChangeId);
            canRevertByUserBefore = otherUserChangeCount == 0;
        }

        // kontrola, že neexistuje změna, který by uživateli znemožnila provést revert (true - neexistuje a můžu provést revert)
        boolean canReverBefore = true;

        if (offset > 0 && isNodeContext) {
            List<ChangeResult> toChangeResultList = findChange(fundId, nodeId, 1, offset, fromChangeId);
            ChangeResult toChangeResult = toChangeResultList.get(0);
            int countBefore = countChangeBefore(fundId, nodeId, fromChangeId, toChangeResult.getChangeId());
            if (countBefore > 0) {
                canReverBefore = false;
            }
        }

        List<Change> changes = convertChangeResults(changeList, fundVersion, fullRevertPermission, canReverBefore, canRevertByUserBefore, isNodeContext);

        // sestavení odpovědi
        ChangesResult changesResult = new ChangesResult();
        changesResult.setMaxSize(maxSize);
        changesResult.setOffset(offset);
        changesResult.setOutdated(fromChange != null && !fromChange.getChangeId().equals(lastChange.getChangeId()));
        changesResult.setTotalCount(count);
        changesResult.setChanges(changes);

        return changesResult;
    }

    /**
     * Vyhledání provedení změn nad AS, případně nad konkrétní JP z AS v závislosti na zvoleném datumu.
     *
     * @param fundVersion verze AS nad kterou provádím vyhledávání
     * @param node        JP omezující vyhledávání
     * @param maxSize     maximální počet záznamů
     * @param fromDate    datum od kterého vyhledávám v historii
     * @param fromChange  změna, vůči které chceme počítat offset - v tomto případě musí být vyplněná
     * @return výsledek hledání
     */
    public ChangesResult findChangesByDate(@NotNull final ArrFundVersion fundVersion,
                                           @Nullable final ArrNode node,
                                           final int maxSize,
                                           @NotNull final OffsetDateTime fromDate,
                                           @NotNull final ArrChange fromChange) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");
        Validate.notNull(fromDate, "Datum od musí být vyplněn");
        Validate.notNull(fromChange, "Změna musí být vyplněna");

        Integer fundId = fundVersion.getFund().getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();
        Integer fromChangeId = fromChange.getChangeId();

        // dotaz pro zjištění pozice v seznamu podle datumu
        int count = countChangeIndex(fundId, nodeId, fromChangeId, fromDate);

        return findChanges(fundVersion, node, maxSize, count, fromChange);
    }

    /**
     * Provede revertování dat ke zvolené změně.
     *
     * @param fundVersion verze AS nad kterým provádím obnovu
     * @param node       JP omezující obnovu
     * @param fromChange změna od které se provádí revert (pouze pro kontrolu, že se jedná o poslední)
     * @param toChange   změna ke které se provádí revert (včetně)
     */
    public void revertChanges(@NotNull final ArrFundVersion fundVersion,
                              @Nullable final ArrNode node,
                              @NotNull final ArrChange fromChange,
                              @NotNull final ArrChange toChange) {
        Validate.notNull(fundVersion, "verze AS musí být vyplněn");
        Validate.isTrue(fundVersion.getLockChange() == null, "Nelze prováděn změny v uzavřené verzi");
        Validate.notNull(fromChange, "Změna od musí být vyplněna");
        Validate.notNull(toChange, "Změna do musí být vyplněna");
        
        StopWatch sw = new StopWatch("revertChanges");

        ArrFund fund = fundVersion.getFund();
        Integer fundId = fundVersion.getFundId();
        Integer nodeId = node == null ? null : node.getNodeId();

        sw.start("revertChangesValidateAction");
        // provede validaci prováděného revertování
        revertChangesValidateAction(fromChange, toChange, fundId, nodeId);        
        sw.stop();

        sw.start("stopConformityInfFundVersions");
        // zastavení probíhajících výpočtů pro validaci uzlů u verzí
        stopConformityInfFundVersions(fund);
        sw.stop();

        // zjisteni uzlu na nez maji zmeny primy dopad
        sw.start("findChangeNodeIdsQuery");
        Query nodeIdsQuery = findChangeNodeIdsQuery(fund, node, toChange);
        Set<Integer> nodeIdsChange = new HashSet<>(nodeIdsQuery.getResultList());
        sw.stop();
        
        sw.start("deleteConformityInfo");
        {
            Query deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_error");
            deleteEntityQuery.executeUpdate();
        }
        {
            Query deleteEntityQuery = createConformityDeleteForeignEntityQuery(fund, node, /*toChange,*/ "arr_node_conformity_missing");
            deleteEntityQuery.executeUpdate();
        }
        {
            Query deleteEntityQuery = createConformityDeleteEntityQuery(fund, node/*, toChange*/);
            deleteEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from usedValueRepository");
        // drop used/fixed values
        usedValueRepository.deleteToChange(fund, toChange.getChangeId());
        sw.stop();

        sw.start("delete from arr_level");
        {
            Query deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_level", toChange);
            deleteEntityQuery.executeUpdate();
            
            Query updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_level", toChange);
            updateEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from arr_node_extension");
        {
            Query deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_extension", toChange);
            deleteEntityQuery.executeUpdate();
            
            Query updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_extension", toChange);
            updateEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from arr_dao_link");
        {
            Query deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_dao_link", toChange);
            deleteEntityQuery.executeUpdate();

            Query updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_dao_link", toChange);
            updateEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from arr_desc_item");
        {
            List<Integer> toReindex = new ArrayList<>(1024);

            // preindexovat zaznamy, ktere mohou byt smazane
            toReindex.addAll(node != null
                    ? descItemRepository.findIdByNodeAndCreatedAfterChange(node, toChange)
                    : descItemRepository.findIdByFundAndCreatedAfterChange(fund, toChange)
            );

            Query deleteEntityQuery = createExtendDeleteEntityQuery(fund, node, "createChange", "arr_desc_item", "arr_item", toChange);
            int count = deleteEntityQuery.executeUpdate();

            logger.debug("Deleted {} item(s), fundId:{}, nodeId:{}", count, fundId, nodeId);

            Query updateEntityQuery = createExtendUpdateEntityQuery(fund, node, "deleteChange", "arr_desc_item", "arr_item", toChange);
            count = updateEntityQuery.executeUpdate();

            logger.debug("Set deleteChange = NULL {} item(s), fundId:{}, nodeId:{}", count, fundId, nodeId);

            TypedQuery<ArrData> arrDataQuery = findChangeArrDataQuery(fund, node, toChange);
            Set<ArrData> arrDataList = new HashSet<>(arrDataQuery.getResultList());

            dataRepository.deleteAll(arrDataList);

            // preindexovat všechny aktualni
            toReindex.addAll(node != null
                    ? descItemRepository.findOpenIdByNodeAndCreatedAfterChange(node)
                    : descItemRepository.findOpenIdByFundAndCreatedAfterChange(fund)
            );

            descriptionItemService.reindexDescItem(toReindex);
        }
        sw.stop();

        sw.start("delete from struct objects");
        if (nodeId == null) {

            structuredItemDelete(fund, toChange);
            structuredItemUpdate(fund, toChange);

            sobjVrequestDelete(fund, toChange);

            structuredObjectUpdate(fund, toChange);
        }
        sw.stop();

        sw.start("delete from arr_file");
        if (nodeId == null) {

            List<Integer> ids = arrFileRepository.findIdByFundAndGreaterOrEqualCreateChange(fund, toChange);
            if (!CollectionUtils.isEmpty(ids)) {
                dmsService.deleteFilesAfterCommitByIds(ids);
            }

            arrFileDeleteChangeUndo(fund, toChange);
            arrFileCreateChangeUndo(fund, toChange);
        }
        sw.stop();

        sw.start("delete from outputs");
        {
            Query updateEntityQuery = createUpdateOutputQuery(fund, node, toChange);
            updateEntityQuery.executeUpdate();
            
            Query deleteEntityQuery = createDeleteOutputItem(fund, toChange);
            deleteEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from arr_node_output");
        {
            Query updateEntityQuery = createSimpleUpdateEntityQuery(fund, node, "deleteChange", "arr_node_output", toChange);
            Query deleteEntityQuery = createSimpleDeleteEntityQuery(fund, node, "createChange", "arr_node_output", toChange);
            updateEntityQuery.executeUpdate();
            deleteEntityQuery.executeUpdate();
        }
        sw.stop();

        sw.start("delete from arr_bulk_action_run");
        {
            Query updateEntityQuery = createUpdateActionQuery(fund, node, toChange);
            updateEntityQuery.executeUpdate();
        }
        {
            Query deleteEntityQuery = createDeleteActionNodeQuery(fund, node, toChange);
            deleteEntityQuery.executeUpdate();
        }
        {
            Query deleteEntityQuery = createDeleteActionRunQuery(fund, toChange);
            deleteEntityQuery.executeUpdate();
        }
        sw.stop();

        // TODO: mazani ARR_CHANGE je velmi pomale
        //  mazani zaznamu je docasne vypnuto
        //  bude nutne najit vhodnejsi reseni
        //sw.start("delete from arr_change");
        //Query deleteNotUseChangesQuery = createDeleteNotUseChangesQuery();
        //deleteNotUseChangesQuery.executeUpdate();
        //sw.stop();

        sw.start("flushing");
        entityManager.flush();

        sw.stop();
        // strukt typy lze smazat az po vymazani vsech ref. na ne
        sw.start("deleting struct objects");
        if (nodeId == null) {

            asyncRequestDelete(fund, toChange);

            dataStructureRefDelete(fund, toChange);

            structuredObjectDelete(fund, toChange);

        }
        sw.stop();

        sw.start("deleting unused nodes");
        // Drop unused node ids
        // Find nodes
        List<Integer> deleteNodeIds = nodeRepository.findUnusedNodeIdsByFund(fund);
        userService.deletePermissionByNodeIds(deleteNodeIds);
        if (CollectionUtils.isNotEmpty(deleteNodeIds)) {
            nodeIdsChange.removeAll(deleteNodeIds);

            issueService.resetIssueNode(deleteNodeIds);

            // drop changes by deleted nodes
            changeRepository.deleteByPrimaryNodeIds(deleteNodeIds);

            // Drop from cache
            nodeCacheService.deleteNodes(deleteNodeIds);
            // Remove from DB

            nodeRepository.deleteByNodeIdIn(deleteNodeIds);
        }
        sw.stop();

        sw.start("flushing 2");
        entityManager.flush();
        sw.stop();

        sw.start("syncing nodes");
        nodeCacheService.syncNodes(nodeIdsChange);
        sw.stop();

        sw.start("publishing changes");
        if (CollectionUtils.isNotEmpty(deleteNodeIds)) {
            eventNotificationService.publishEvent(new EventIdsInVersion(EventType.DELETE_NODES, fundVersion.getFundVersionId(),
                    deleteNodeIds.toArray(new Integer[deleteNodeIds.size()])));
        }        

        if (node == null) {
            eventNotificationService.publishEvent(new EventFunds(EventType.FUND_INVALID, 
            		Collections.singleton(fundId), Collections.singleton(fundVersion.getFundVersionId())));
        } else {
        	eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, fundVersion.getFundVersionId(), node.getNodeId()));
        }
        sw.stop();

        sw.start("invalidateFundVersion");
        levelTreeCacheService.invalidateFundVersion(fundVersion);
        sw.stop();
        sw.start("startNodeValidation");
        arrangementService.startNodeValidation(fundVersion);
        sw.stop();
        
        // log results
        if(logger.isDebugEnabled()) {
        	logger.debug("revertChanges - progress\n{}", sw.prettyPrint());
        }
    }

    private TypedQuery<ArrData> findChangeArrDataQuery(final ArrFund fund, final ArrNode node, final ArrChange change) {

        String changeNameColumn = "createChange";
        String table = "arr_desc_item";
        String subTable = "arr_item";

        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hql = String.format("SELECT i.data FROM %1$s i WHERE i.%2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        TypedQuery<ArrData> query = entityManager.createQuery(hql, ArrData.class);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    /**
     * Zjištění počtu položek, po kterých nelze provést revert v rámci JP.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @param toChangeId   identifikátor změny, vůči které provádíme vyhledávání
     * @return počet změn
     */
    private int countChangeBefore(@NotNull Integer fundId, @Nullable Integer nodeId, @Nullable Integer fromChangeId, @NotNull Integer toChangeId) {

        String selectParams = "COUNT(*)";
        String querySpecification = "";

        List<String> wheres = new ArrayList<>();

        if (fromChangeId != null) {
            wheres.add("ch.change_id <= :fromChangeId");
        }

        wheres.add("ch.change_id >= :toChangeId");

        wheres.add("ch.type NOT IN (:types)");

        if (wheres.size() > 0) {
            querySpecification = "WHERE " + String.join(" AND ", wheres) + " " + querySpecification;
        }

        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification);

        List<String> allowedChangeTypes = new ArrayList<>();
        allowedChangeTypes.add(ArrChange.Type.UPDATE_DESC_ITEM.name());
        allowedChangeTypes.add(ArrChange.Type.ADD_DESC_ITEM.name());
        allowedChangeTypes.add(ArrChange.Type.DELETE_DESC_ITEM.name());

        if (nodeId == null) {
            allowedChangeTypes.add(ArrChange.Type.ADD_STRUCTURE_DATA.name());
            allowedChangeTypes.add(ArrChange.Type.ADD_STRUCTURE_DATA_BATCH.name());
            allowedChangeTypes.add(ArrChange.Type.UPDATE_STRUCT_DATA_BATCH.name());
            allowedChangeTypes.add(ArrChange.Type.DELETE_STRUCTURE_DATA.name());
            allowedChangeTypes.add(ArrChange.Type.ADD_STRUCTURE_ITEM.name());
            allowedChangeTypes.add(ArrChange.Type.UPDATE_STRUCTURE_ITEM.name());
            allowedChangeTypes.add(ArrChange.Type.DELETE_STRUCTURE_ITEM.name());
        }

        query.setParameter("types", allowedChangeTypes);

        query.setParameter("fundId", fundId);

        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }

        query.setParameter("toChangeId", toChangeId);

        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Provádí validaci akce pro revertování změn.
     *
     * @param fromChange změna od které se provádí revert (pouze pro kontrolu, že se jedná o poslední)
     * @param toChange   změna ke které se provádí revert
     * @param fundId     identifikátor AS
     * @param nodeId     identifikátor JP
     */
    private void revertChangesValidateAction(final @NotNull ArrChange fromChange,
                                             final @NotNull ArrChange toChange,
                                             final @NotNull Integer fundId,
                                             final @Nullable Integer nodeId) {
        // dotaz pro zjištění poslední změny
        ChangeResult lastChange = getLastChange(fundId, nodeId);
        if (lastChange == null) {
            throw new BusinessException("Failed to find valid last change", ArrangementCode.DATA_NOT_FOUND)
                    .set("nodeId", nodeId);
        }

        if (!fromChange.getChangeId().equals(lastChange.getChangeId())) {
            throw new BusinessException("Existuje novější verze", ArrangementCode.EXISTS_NEWER_CHANGE);
        }

        if (toChange.getType() != null && toChange.getType().equals(ArrChange.Type.CREATE_AS)) {
            throw new BusinessException("Existuje blokující změna v JP", ArrangementCode.EXISTS_BLOCKING_CHANGE);
        }

        if (nodeId != null) {

            int countBefore = countChangeBefore(fundId, nodeId, fromChange.getChangeId(), toChange.getChangeId());
            if (countBefore > 0) {
                throw new BusinessException("Existuje blokující změna v JP", ArrangementCode.EXISTS_BLOCKING_CHANGE);
            }

            // check if change includes structured types
            /*
            int itemsCnt = structuredItemRepository.countItemsWithinChangeRange(fundId, fromChange.getChangeId(), toChange.getChangeId());
            if (itemsCnt > 0) {
                throw new BusinessException("Existuje změna ve strukturovaném objektu", ArrangementCode.EXISTS_BLOCKING_CHANGE);
            }

            int objectsCnt = structuredObjectRepository.countItemsWithinChangeRange(fundId, fromChange.getChangeId(), toChange.getChangeId());
            if (objectsCnt > 0) {
                throw new BusinessException("Existuje změna ve strukturovaném objektu", ArrangementCode.EXISTS_BLOCKING_CHANGE);
            }
            */
        }
    }

    private Query createUpdateOutputQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("UPDATE arr_output o" +
                " SET o.state = :stateNew" +
                " WHERE o.state IN (:stateOld)" +
                " AND o IN (SELECT no.output FROM arr_node_output no WHERE no.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND no.createChange >= :change OR no.deleteChange >= :change)");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        query.setParameter("stateNew", ArrOutput.OutputState.OUTDATED);
        query.setParameter("stateOld", Collections.singletonList(ArrOutput.OutputState.FINISHED));
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Odstranění ArrOutputItem spojených s odstraněnými strukturálními objekty.
     * 
     * @param fund      AS nad kterým provádím obnovu
     * @param toChange  změna ke které se provádí revert
     * @return
     */
    private Query createDeleteOutputItem(final @NotNull ArrFund fund, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_output_item oi WHERE oi.itemId in (" +
                " SELECT i.itemId FROM arr_item i" +
                " JOIN arr_data_structure_ref r ON i.dataId = r.dataId" +
                " JOIN arr_structured_object so ON so.structuredObjectId = r.structuredObjectId" +
                " WHERE so.fund = :fund AND so.createChange >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);

        return query;
    }

    /**
     * Smazání návazných entity hromadné akce.
     *
     * @param fund     AS nad kterým provádím obnovu
     * @param node     JP omezující obnovu
     * @param toChange změna ke které se provádí revert
     * @return
     */
    private Query createDeleteActionNodeQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_bulk_action_node n WHERE n.bulkActionRun IN (" +
                "SELECT rn.bulkActionRun FROM arr_bulk_action_node rn JOIN rn.bulkActionRun rs WHERE rn.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND rs.change >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Smazání samotných hromadných akcí.
     *
     * @param fund     AS nad kterým provádím obnovu
     * @param toChange změna ke které se provádí revert
     * @return
     */
    private Query createDeleteActionRunQuery(final @NotNull ArrFund fund, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("DELETE FROM arr_bulk_action_run rd WHERE rd IN (" +
                " SELECT r FROM arr_bulk_action_run r" +
                " JOIN r.fundVersion v" +
                " WHERE v.fund = :fund AND r.change >= :change" +
                ")");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);

        return query;
    }

    private Query createUpdateActionQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, final @NotNull ArrChange toChange) {
        Query query = entityManager.createQuery("UPDATE arr_bulk_action_run r SET r.state = :stateNew WHERE r IN (" +
                "SELECT rs FROM arr_bulk_action_run rs JOIN rs.arrBulkActionNodes rn WHERE rn.node IN (" + createHqlSubNodeQuery(fund, node) + ") AND rs.change >= :change" +
                ") AND r.state = :stateOld");

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);
        query.setParameter("stateNew", ArrBulkActionRun.State.OUTDATED);
        query.setParameter("stateOld", ArrBulkActionRun.State.FINISHED);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Zastavení probíhajících výpočtů pro validaci uzlů u verzí.
     *
     * @param fund AS nad kterým provádím zastavení
     */
    private void stopConformityInfFundVersions(final @NotNull ArrFund fund) {
        for (ArrFundVersion fundVersion : fund.getVersions()) {
            asyncRequestService.terminateNodeWorkersByFund(fundVersion.getFundVersionId());
        }
    }

    private Query createConformityDeleteEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node/*, final @NotNull ArrChange toChange*/) {
        //String nodesHql = createHQLFindChanges("createChange", "arr_level", );
        //String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM arr_node_conformity WHERE node IN (%1$s)", createHqlSubNodeQuery(fund, node));
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        //query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    private Query createConformityDeleteForeignEntityQuery(final @NotNull ArrFund fund, final @Nullable ArrNode node, /*final @NotNull ArrChange toChange,*/ final @NotNull String table) {
        //String nodesHql = createHQLFindChanges("createChange", "arr_level", createHqlSubNodeQuery(fund, node));
        //String hqlSubSelect = String.format("SELECT i.node FROM %1$s i WHERE %2$s IN (%3$s)", "arr_level", "createChange", nodesHql);
        String hql = String.format("DELETE FROM %1$s ncx WHERE ncx.nodeConformity IN (SELECT nc FROM arr_node_conformity nc WHERE node IN (%2$s))", table, createHqlSubNodeQuery(fund, node));
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        //query.setParameter("change", toChange);
        if (node != null) {
            query.setParameter("node", node);
        }
        return query;
    }

    /**
     * Delete from ARR_CHANGE unused change_ids
     *
     * @return
     */
    public Query createDeleteNotUseChangesQuery(@Nullable final Integer ignoreChangeId) {

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(
                          "DELETE FROM arr_change c WHERE c.change_id IN (SELECT distinct c.change_id FROM arr_change c");
        sqlBuilder.append(" LEFT JOIN (");

        String[][] configUnionTables = new String[][]{
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_CREATE_CHANGE_ID },
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_DELETE_CHANGE_ID },
                { ArrItem.TABLE_NAME, ArrItem.FIELD_CREATE_CHANGE_ID },
                { ArrItem.TABLE_NAME, ArrItem.FIELD_DELETE_CHANGE_ID },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_CREATE_CHANGE_ID },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_DELETE_CHANGE_ID },

                { ArrFundVersion.TABLE_NAME, ArrFundVersion.FIELD_CREATE_CHANGE_ID },
                { ArrFundVersion.TABLE_NAME, ArrFundVersion.FIELD_LOCK_CHANGE_ID },
                { ArrBulkActionRun.TABLE_NAME, ArrBulkActionRun.FIELD_CHANGE_ID },
                { ArrOutput.TABLE_NAME, ArrOutput.FIELD_CREATE_CHANGE_ID },
                { ArrOutput.TABLE_NAME, ArrOutput.FIELD_DELETE_CHANGE_ID },
                { ArrNodeOutput.TABLE_NAME, ArrNodeOutput.FIELD_CREATE_CHANGE_ID },
                { ArrNodeOutput.TABLE_NAME, ArrNodeOutput.FIELD_DELETE_CHANGE_ID },
                { ArrOutputResult.TABLE_NAME, ArrOutputResult.FIELD_CHANGE_ID },

                { ArrFile.TABLE_NAME, ArrFile.FIELD_CREATE_CHANGE_ID },
                { ArrFile.TABLE_NAME, ArrFile.FIELD_DELETE_CHANGE_ID },

                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_CREATE_CHANGE_ID },
                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_DELETE_CHANGE_ID },

                { ArrRequestQueueItem.TABLE_NAME, ArrRequestQueueItem.FIELD_CREATE_CHANGE_ID },
                { ArrRequest.TABLE_NAME, ArrRequest.FIELD_CREATE_CHANGE_ID },

                { ArrStructuredObject.TABLE_NAME, ArrStructuredObject.FIELD_CREATE_CHANGE_ID },
                { ArrStructuredObject.TABLE_NAME, ArrStructuredObject.FIELD_DELETE_CHANGE_ID },

                { ArrFundStructureExtension.TABLE_NAME, ArrFundStructureExtension.CREATE_CHANGE_ID },
                { ArrFundStructureExtension.TABLE_NAME, ArrFundStructureExtension.DELETE_CHANGE_ID }
        };

        NamingStrategy ins = ImprovedNamingStrategy.INSTANCE;

        List<String> unionPart = new ArrayList<>(configUnionTables.length);
        for (int index = 0; index < configUnionTables.length; index++) {
            String[] changeTable = configUnionTables[index];

            String columnName = ins.columnName(changeTable[1]);

            unionPart.add(String.format("SELECT distinct t%1$d.%3$s as change_id FROM %2$s t%1$d", index,
                                        changeTable[0],
                                        columnName));
        }
        sqlBuilder.append(String.join("\nUNION\n", unionPart));
        sqlBuilder.append(") as used_change ON c.change_id = used_change.change_id ");
        sqlBuilder.append("WHERE used_change.change_id IS NULL");
        sqlBuilder.append(")");
        if (ignoreChangeId != null) {
            sqlBuilder.append(" AND c.change_id <> ").append(ignoreChangeId);
        }

        String sql = sqlBuilder.toString();

        logger.debug("Prepared query: {}", sql);

        return entityManager.createNativeQuery(sql);
    }

    /**
     * Delete from ARR_CHANGE unused change_ids
     *
     * @return
     */
    public Query createDeleteNotUseChangesQuery() {
        return createDeleteNotUseChangesQuery(null);
    }

    private Query createExtendUpdateEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final String subTable,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hqlSubSelect = String.format("SELECT i.%2$s FROM %1$s i WHERE %2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        String hql = String.format("UPDATE %1$s SET %2$s = NULL WHERE %2$s IN (%3$s)", table, changeNameColumn, hqlSubSelect);
        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private void structuredItemDelete(@NotNull ArrFund fund, @NotNull ArrChange toChange) {

        String hql = "DELETE FROM arr_structured_item i WHERE i.itemId IN (" +
                " SELECT si.itemId FROM arr_structured_item si" +
                " JOIN si.structuredObject so" +
                " WHERE so.fund = :fund" +
                " AND si.createChange >= :change" +
                ")";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void structuredItemUpdate(@NotNull final ArrFund fund, @NotNull final ArrChange change) {

        List<ArrStructuredItem> structuredItemList = structuredItemRepository.findNotBeforeDeleteChange(fund, change);

        structObjValueService.addToValidate(structuredItemList.stream().map(item -> item.getStructuredObject()).collect(Collectors.toList()));

        for (ArrStructuredItem structuredItem : structuredItemList) {
            structuredItem.setDeleteChange(null);
        }

        structuredItemRepository.saveAll(structuredItemList);
    }

    private void sobjVrequestDelete(@NotNull ArrFund fund, @NotNull ArrChange toChange) {

        String hql = "DELETE FROM arr_sobj_vrequest r" +
                " WHERE r.structuredObject IN (" +
                " SELECT so FROM arr_structured_object so" +
                " WHERE so.fund = :fund" +
                " AND so.createChange >= :change" +
                ")";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void asyncRequestDelete(@NotNull ArrFund fund, @NotNull ArrChange toChange) {
        
        String hql = "DELETE FROM arr_async_request r" +
                " WHERE r.structuredObject IN (" +
                " SELECT so FROM arr_structured_object so" +
                " WHERE so.fund = :fund" +
                " AND so.createChange >= :change" +
                ")";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void dataStructureRefDelete(@NotNull ArrFund fund, @NotNull ArrChange toChange) {

        String hql = "DELETE FROM arr_data_structure_ref r" +
                " WHERE r.structuredObject IN (" +
                " SELECT so FROM arr_structured_object so" +
                " WHERE so.fund = :fund" +
                " AND so.createChange >= :change" +
                ")";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void structuredObjectDelete(@NotNull ArrFund fund, @NotNull ArrChange toChange) {

        String hql = "DELETE FROM arr_structured_object so" +
                " WHERE so.fund = :fund" +
                " AND so.createChange >= :change";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void arrFileDeleteChangeUndo(@NotNull final ArrFund fund, @NotNull final ArrChange toChange) {

        String hql = "UPDATE arr_file af SET af.deleteChange = NULL" +
                " WHERE af.fund = :fund AND af.deleteChange >= :change";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void arrFileCreateChangeUndo(@NotNull final ArrFund fund, @NotNull final ArrChange toChange) {

        String hql = "DELETE FROM arr_file af" +
                " WHERE af.fund = :fund AND af.createChange >= :change";

        executeRequestWithParameters(hql, fund, toChange);
    }

    private void executeRequestWithParameters(@NotNull String hql, @NotNull ArrFund fund, @NotNull ArrChange toChange) {

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", toChange);

        query.executeUpdate();
    }

    private void structuredObjectUpdate(@NotNull final ArrFund fund, @NotNull final ArrChange change) {

        List<ArrStructuredObject> structuredObjectList = structuredObjectRepository.findNotBeforeDeleteChange(fund, change);

        structObjValueService.addToValidate(structuredObjectList);

        List<Integer> structureDataIds = new ArrayList<>(structuredObjectList.size());
        for (ArrStructuredObject structuredObject : structuredObjectList) {
            structuredObject.setDeleteChange(null);
            structureDataIds.add(structuredObject.getStructuredObjectId());
        }

        structuredObjectRepository.saveAll(structuredObjectList);

        eventNotificationService.publishEvent(new EventStructureDataChange(fund.getFundId(),
                null,
                null,
                null,
                structureDataIds,
                null));
    }

    /**
     * Vytvoreni dotazy zjistujiciho zmenene node do dane change
     * 
     * @param fund
     * @param node
     * @param change
     * @return
     */
    private Query findChangeNodeIdsQuery(@NotNull final ArrFund fund,
                                         @Nullable final ArrNode node,
                                         @NotNull final ArrChange change) {
        // pole tabulek a sloupcu, kde je zjistovana zmena vcetne vazby na node
        String[][] tables = new String[][]{
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_NODE,
                        ArrLevel.FIELD_CREATE_CHANGE },
                { ArrLevel.TABLE_NAME, ArrLevel.FIELD_NODE,
                        ArrLevel.FIELD_DELETE_CHANGE },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_NODE, ArrNodeExtension.FIELD_CREATE_CHANGE_ID },
                { ArrNodeExtension.TABLE_NAME, ArrNodeExtension.FIELD_NODE, ArrNodeExtension.FIELD_DELETE_CHANGE_ID },
                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_NODE, ArrDaoLink.FIELD_CREATE_CHANGE_ID },
                { ArrDaoLink.TABLE_NAME, ArrDaoLink.FIELD_NODE,
                        ArrDaoLink.FIELD_DELETE_CHANGE_ID },
                { ArrDescItem.TABLE_NAME,
                        ArrDescItem.FIELD_NODE, ArrDescItem.FIELD_CREATE_CHANGE_ID },
                { ArrDescItem.TABLE_NAME,
                        ArrDescItem.FIELD_NODE,
                        ArrDescItem.FIELD_DELETE_CHANGE_ID },
        };

        List<String> hqls = new ArrayList<>();
        for (String[] table : tables) {
            String nodesHql = createHQLFindChanges(table[2], table[0], createHqlSubNodeQuery(fund, node));
            String hql = String.format("SELECT i.nodeId FROM %1$s i WHERE %2$s IN (%3$s)", table[0], table[2],
                                       nodesHql);
            hqls.add(hql);
        }

        String hql = "SELECT DISTINCT n.nodeId FROM arr_node n WHERE n.nodeId IN (" + String.join(") OR n.nodeId IN (", hqls) + ")";

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createExtendDeleteEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final String subTable,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));

        String hqlSubSelect = String.format("SELECT i.%2$s FROM %1$s i WHERE %2$s IN (%3$s)", subTable, changeNameColumn, nodesHql);
        String hql = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", table, changeNameColumn, hqlSubSelect);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createSimpleUpdateEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));
        String hql = String.format("UPDATE %1$s SET %2$s = NULL WHERE %2$s IN (%3$s)", table, changeNameColumn, nodesHql);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private Query createSimpleDeleteEntityQuery(@NotNull final ArrFund fund,
                                                @Nullable final ArrNode node,
                                                @NotNull final String changeNameColumn,
                                                @NotNull final String table,
                                                @NotNull final ArrChange change) {
        String nodesHql = createHQLFindChanges(changeNameColumn, table, createHqlSubNodeQuery(fund, node));
        String hql = String.format("DELETE FROM %1$s WHERE %2$s IN (%3$s)", table, changeNameColumn, nodesHql);

        Query query = entityManager.createQuery(hql);

        // nastavení parametrů dotazu
        query.setParameter("fund", fund);
        query.setParameter("change", change);
        if (node != null) {
            query.setParameter("node", node);
        }

        return query;
    }

    private String createHQLFindChanges(@NotNull final String changeNameColumn,
                                        @NotNull final String table,
                                        @NotNull final String hqlNodes) {
        return String.format("SELECT DISTINCT c.%1$s FROM %2$s c WHERE c.node IN (%3$s) AND %1$s >= :change",
                changeNameColumn, table, hqlNodes);
    }

    /**
     * Zjistí, jestli má oprávnění na provedení všech změn.
     *
     * @param fund AS u kterého kontrolujeme oprávnění
     * @return má oprávnění provádět obnovu všech změn
     */
    private boolean hasFullRevertPermission(@NotNull final ArrFund fund) {
        return userService.hasPermission(UsrPermission.Permission.ADMIN)
                || userService.hasPermission(UsrPermission.Permission.FUND_ADMIN)
                || userService.hasPermission(UsrPermission.Permission.FUND_VER_WR, fund.getFundId());
    }

    /**
     * Převedení změn z databázového dotazu na změny pro odpověď.
     *
     * @param changeResultList      změny z dotazu
     * @param fundVersion           verze AS
     * @param fullRevertPermission  má úplné oprávnění?
     * @param isNodeContext         změny jsou v kontextu JP
     * @param canReverBefore        může se revertovat změna? (true - předchozí můžou)
     * @param canRevertByUserBefore může se revertovat změna - uživatel? (true - předchozí provedl stejný uživatel)
     * @return seznam změn pro odpověď
     */
    private List<Change> convertChangeResults(final List<ChangeResult> changeResultList,
                                              final ArrFundVersion fundVersion,
                                              final boolean fullRevertPermission,
                                              final boolean canReverBefore,
                                              final boolean canRevertByUserBefore,
                                              final boolean isNodeContext) {
        UsrUser loggedUser = userService.getLoggedUser();

        boolean canRevert = canReverBefore;
        boolean canRevertByUser = canRevertByUserBefore;

        List<Change> changes = new ArrayList<>(changeResultList.size());

        HashMap<Integer, Integer> changeIdNodeIdMap = new HashMap<>();

        Set<Integer> userIds = new HashSet<>();

        for (ChangeResult changeResult : changeResultList) {
            Integer primaryNodeId = changeResult.getPrimaryNodeId();
            if (primaryNodeId != null) {
                changeIdNodeIdMap.put(changeResult.changeId, changeResult.primaryNodeId);
            }
            Integer userId = changeResult.getUserId();
            if (userId != null) {
                userIds.add(userId);
            }
        }

        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(), fundVersion.getFundId());
        
        // TODO is it still needed?
        List<RulItemType> descItemTypes;
        if(CollectionUtils.isEmpty(viewTitles.getTreeItem().getIds())) {
            descItemTypes = Collections.emptyList();
        } else {
            StaticDataProvider dataProvider = staticDataService.getData();
            descItemTypes = new ArrayList<>(viewTitles.getTreeItem().getIds().size());
            for(Integer itemTypeId: viewTitles.getTreeItem().getIds()) {
                RulItemType itemType = dataProvider.getItemType(itemTypeId);
                Validate.notNull(itemType, "Missing item type, id: %s", itemTypeId);
                descItemTypes.add(itemType);
            }
        }

        // TODO předělat createNodeLabels()
        HashMap<Map.Entry<Integer, Integer>, String> changeNodeMap = createNodeLabels(changeIdNodeIdMap, descItemTypes,
                fundVersion.getRuleSetId(),
                fundVersion.getFund()
                        .getFundId());

        Map<Integer, Map<Integer, ArrStructuredObject>> changeIdStructuredObjectMap = structObjService.groupStructuredObjectByChange(
                fundVersion.getFundId(),
                changeResultList.stream().map(change -> change.getChangeId()).collect(Collectors.toList()));

        Map<Integer, UsrUser> users = userService.findUserMap(userIds);

        List<ArrChange.Type> allowedNodeChangeTypes = Arrays.asList(
                ArrChange.Type.UPDATE_DESC_ITEM,
                ArrChange.Type.ADD_DESC_ITEM,
                ArrChange.Type.DELETE_DESC_ITEM);

        for (ChangeResult changeResult : changeResultList) {
            Change change = new Change();
            change.setChangeId(changeResult.changeId);
            change.setNodeChanges(changeResult.nodeChanges == null ? null : changeResult.nodeChanges.intValue());
            change.setChangeDate(Date.from(changeResult.changeDate.toInstant()));
            change.setPrimaryNodeId(changeResult.primaryNodeId);
            change.setType(StringUtils.isEmpty(changeResult.type) ? null : ArrChange.Type.valueOf(changeResult.type));
            change.setUserId(changeResult.userId);

            // nemůžu revertovat vytvoření AS
            if (change.getType() == null || change.getType().equals(ArrChange.Type.CREATE_AS)) {
                canRevert = false;
            }

            if (isNodeContext) {
                if (change.getType() != null && !allowedNodeChangeTypes.contains(change.getType())
                        || change.getNodeChanges() > 1) {
                    canRevert = false;
                }
            }

            UsrUser usrUser = null;
            if (changeResult.userId != null) {
                usrUser = users.get(changeResult.userId);
                change.setUsername(usrUser.getUsername());
            }

            // pokud nemám plné oprávnění vracet změny a pokud nejsem uživatel provedené změny, nemůžu provést změny
            if (!fullRevertPermission && !loggedUser.equals(usrUser)) {
                canRevertByUser = false;
            }

            change.setRevert(canRevert && canRevertByUser);

            String label = changeNodeMap.get(new AbstractMap.SimpleImmutableEntry<>(changeResult.changeId, changeResult.primaryNodeId));
            if (label == null) {
                // zkontrolovat, zda se nejedna o zmenu ve strukturovanem typu
                Map<Integer, ArrStructuredObject> structuredObjectMap = changeIdStructuredObjectMap.get(changeResult.changeId);
                if (structuredObjectMap != null) {
                    label = structuredObjectMap.values()
                            .stream()
                            .sorted(Comparator.comparing(object -> object.getSortValue()))
                            .map(object -> object.getValue())
                            .collect(Collectors.joining(" "));
                }
            }
            change.setLabel(label);
            changes.add(change);
        }
        return changes;
    }

    /**
     * Vytvoření mapy popisků JP podle identifikátoru změny/JP.
     *
     * @param changeIdNodeIdMap
     *            mapa změny/JP
     * @param itemTypes
     *            seznam typů atributů
     * @param ruleSetId
     *            identifikátor pravidel
     * @param fundId
     *            @return mapa popisků
     */
    private HashMap<Map.Entry<Integer, Integer>, String> createNodeLabels(final HashMap<Integer, Integer> changeIdNodeIdMap,
                                                                          final List<RulItemType> itemTypes,
                                                                          final Integer ruleSetId,
                                                                          final Integer fundId) {
        HashMap<Map.Entry<Integer, Integer>, String> result = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : changeIdNodeIdMap.entrySet()) {
            Integer nodeId = entry.getValue();
            Map<Integer, TitleItemsByType> nodeValuesMap = descriptionItemService
                    .createNodeValuesByItemTypeCodeMap(Collections.singleton(nodeId), itemTypes, entry.getKey(), null);
            TitleItemsByType items = nodeValuesMap.get(nodeId);
            if (items != null) {
                List<String> titles = new ArrayList<>();
                for (RulItemType itemType : itemTypes) {
                    titles.addAll(items.getValues(itemType.getItemTypeId()));
                }
                result.put(entry, String.join(" ", titles));
            } else {
                ViewTitles viewTitles = configView.getViewTitles(ruleSetId, fundId);
                String defaultTitle = viewTitles.getDefaultTitle();
                defaultTitle = StringUtils.isEmpty(defaultTitle) ? "JP <" + nodeId + ">" : defaultTitle;
                result.put(entry, defaultTitle);
            }
        }

        return result;
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fund AS
     * @param node JP
     * @return HQL řetězec
     */
    private String createHqlSubNodeQuery(@NotNull final ArrFund fund,
                                         @Nullable final ArrNode node) {
        Validate.notNull(fund, "AS musí být vyplněn");
        String query = "SELECT n FROM arr_node n WHERE fund = :fund";
        if (node != null) {
            query += " AND n = :node";
        }
        return query;
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @return SQL řetězec
     */
    private Query createFindChangeQuery(String selectParams, @NotNull Integer fundId, @Nullable Integer nodeId, String querySpecification, String mapping) {
        return createFindChangeQuery(selectParams, fundId, nodeId, false, querySpecification, mapping);
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @return SQL řetězec
     */
    private Query createFindChangeQuery(String selectParams, @NotNull Integer fundId, @Nullable Integer nodeId, String querySpecification) {
        return createFindChangeQuery(selectParams, fundId, nodeId, false, querySpecification, null);
    }

    /**
     * Sestavení řetězce pro vnořený dotaz, který vrací seznam JP omezený AS nebo JP.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @param excludeAction vynechat změny, které byly provedeny v rámci hromadných akcí
     * @return SQL řetězec
     */
    private Query createFindChangeQuery(String selectParams, @NotNull Integer fundId, @Nullable Integer nodeId, boolean excludeAction, String querySpecification, String mapping) {

        Validate.notNull(fundId, "Identifikátor AS musí být vyplněn");

        StringBuilder nodeSubquery = new StringBuilder(256);
        nodeSubquery.append("SELECT node_id FROM arr_node WHERE fund_id = :fundId");
        if (nodeId != null) {
            nodeSubquery.append(" AND node_id = :nodeId");
        }

        StringBuilder sqlTemplate = new StringBuilder(1024);
        sqlTemplate.append("SELECT  \n" +
                "%1$s\n" +
                "FROM\n" +
                "  arr_change ch\n" +
                "JOIN \n" +
                "(\n" +
                "  SELECT change_id, COUNT(*) AS node_changes, SUM(weight) AS weights FROM (\n" + // provádním agregaci přes součet vah a počtu unikátnách změn
                "    SELECT DISTINCT change_id, node_id AS nodes, weight FROM (\n" + // provádím union změn z požadovaných tabulek, u hromadných akcí má změna váhu 0, protože upravené změny se počítají přes hodnoty atributů
                "      SELECT create_change_id AS change_id, node_id, 1 AS weight FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id AS change_id, node_id, 1 AS weight FROM arr_level WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_desc_item di JOIN arr_item i ON i.item_id = di.item_id WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_extension WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_extension WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_node_output WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT create_change_id, node_id, 1 AS weight FROM arr_dao_link WHERE node_id IN (%2$s)\n" +
                "      UNION ALL\n" +
                "      SELECT delete_change_id, node_id, 1 AS weight FROM arr_dao_link WHERE node_id IN (%2$s)\n");

        if (nodeId == null) {
            sqlTemplate.append(
                    "      UNION ALL\n" +
                    "      SELECT i.create_change_id, null, 1 AS weight FROM arr_structured_item si\n" +
                    "            JOIN arr_item i ON i.item_id = si.item_id\n" +
                    "            JOIN arr_structured_object so ON so.structured_object_id = si.structured_object_id\n" +
                    "            WHERE so.fund_id = :fundId\n" +
                    "      UNION ALL\n" +
                    "      SELECT i.delete_change_id, null, 1 AS weight FROM arr_structured_item si\n" +
                    "            JOIN arr_item i ON i.item_id = si.item_id\n" +
                    "            JOIN arr_structured_object so ON so.structured_object_id = si.structured_object_id\n" +
                    "            WHERE so.fund_id = :fundId\n" +
                    "      UNION ALL\n" +
                    "      SELECT create_change_id, null, 1 AS weight FROM arr_file af WHERE af.fund_id = :fundId\n" +
                    "      UNION ALL\n" +
                    "      SELECT delete_change_id, null, 1 AS weight FROM arr_file af WHERE af.fund_id = :fundId\n" +
                    "      UNION ALL\n" +
                    "      SELECT delete_change_id, null, 1 AS weight FROM arr_structured_object so WHERE so.fund_id = :fundId AND so.state <> '" + ArrStructuredObject.State.TEMP.name() + "'\n" +
                    "      UNION ALL\n" +
                    "      SELECT create_change_id, null, 1 AS weight FROM arr_structured_object so WHERE so.fund_id = :fundId AND so.state <> '" + ArrStructuredObject.State.TEMP.name() + "'\n");
        }

        if (!excludeAction) {
            sqlTemplate.append(
                    "      UNION ALL\n" +
                    "      SELECT change_id, null, 0 AS weight FROM arr_bulk_action_run r JOIN arr_fund_version v ON r.fund_version_id = v.fund_version_id WHERE v.fund_id = :fundId AND r.state = '" + ArrBulkActionRun.State.FINISHED + "'\n");
        }

        sqlTemplate.append(
                //                "    ) chlx ORDER BY change_id DESC\n" +
                "    ) chlx \n" +
                "  ) chlxx GROUP BY change_id \n" +
                ") chl\n" +
                "ON\n" +
                "  ch.change_id = chl.change_id\n" +
                "%3$s");

        String queryString = String.format(sqlTemplate.toString(), selectParams, nodeSubquery, querySpecification);
        return mapping == null ? entityManager.createNativeQuery(queryString) : entityManager.createNativeQuery(queryString, mapping);
    }

    /**
     * Vyhledání změn.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param maxSize      maximální počet záznamů
     * @param offset       počet přeskočených záznamů
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return počet změn
     */
    private List<ChangeResult> findChange(@NotNull Integer fundId, @Nullable Integer nodeId, int maxSize, int offset, @Nullable Integer fromChangeId) {

        // doplňující parametry dotazu
        String selectParams = "ch.change_id as changeId, ch.change_date AS changeDate, ch.user_id as userId, ch.type as type, ch.primary_node_id as primaryNodeId, chl.node_changes as nodeChanges, chl.weights as weights";
        String querySpecification = "GROUP BY ch.change_id, ch.change_date, ch.user_id, ch.type, ch.primary_node_id, chl.node_changes, chl.weights ORDER BY ch.change_id DESC";
        if (fromChangeId != null) {
            querySpecification = "WHERE ch.change_id <= :fromChangeId " + querySpecification;
        }

        // vnoření parametrů a vytvoření query objektu
        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification, "ChangeResultMapping");

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        if (maxSize > 0) {
            query.setMaxResults(maxSize);
        }
        query.setFirstResult(offset);

        return query.getResultList();
    }

    /**
     * Vyhledá počet změn, které nevytvořil přihlášený uživatel.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return počet změn
     */
    private int countUserChange(@NotNull Integer fundId, @Nullable Integer nodeId, int maxSize, @Nullable Integer fromChangeId) {

        UsrUser loggedUser = userService.getLoggedUser();

        // doplňující parametry dotazu
        String selectParams = "COUNT(ch.change_id)";
        String querySpecification = "GROUP BY ch.change_id";
        List<String> wheres = new ArrayList<>();
        if (fromChangeId != null) {
            wheres.add("ch.change_id <= :fromChangeId");
        }

        if (loggedUser.getUserId() != null) {
            wheres.add("ch.user_id <> :userId");
        }

        if (wheres.size() > 0) {
            querySpecification = "WHERE " + String.join(" AND ", wheres) + " " + querySpecification;
        }

        // vnoření parametrů a vytvoření query objektu
        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }
        if (loggedUser.getUserId() != null) {
            query.setParameter("userId", loggedUser.getUserId());
        }
        if (maxSize > 0) {
            query.setMaxResults(maxSize);
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Zjištění počtu změn, které se mují přeskočit na základě vyhledání podle datumu.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @param fromDate     datum, od kterého počítám změny k přeskočení (včetně)
     * @return počet změn
     */
    private int countChangeIndex(@NotNull Integer fundId, @Nullable Integer nodeId, @NotNull Integer fromChangeId, @NotNull OffsetDateTime fromDate) {

        // doplňující parametry dotazu
        String selectParams = "COUNT(*)";
        String querySpecification = "WHERE ch.change_id < :fromChangeId AND ch.change_date >= :changeDate";

        // vnoření parametrů a vytvoření query objektu
        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification);

        // nastavení parametrů dotazu
        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        query.setParameter("fromChangeId", fromChangeId);
        query.setParameter("changeDate", Timestamp.valueOf(fromDate.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()), TemporalType.TIMESTAMP);

        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Zjištění poslední provedené změny.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @return změna
     */
    private ChangeResult getLastChange(@NotNull Integer fundId, @Nullable Integer nodeId) {
        List<ChangeResult> changeResultList = findChange(fundId, nodeId, 1, 0, null);
        return changeResultList.size() > 0 ? changeResultList.get(0) : null;
    }

    /**
     * Zjištění celkového počtu změn.
     *
     * @param fundId       identifikátor AS
     * @param nodeId       identifikátor JP
     * @param fromChangeId identifikátor změny, vůči které provádíme vyhledávání
     * @return počet změn
     */
    private int countChange(@NotNull Integer fundId, @Nullable Integer nodeId, @Nullable Integer fromChangeId) {

        String selectParams = "COUNT(*)";
        String querySpecification = "";

        if (fromChangeId != null) {
            querySpecification = "WHERE ch.change_id <= :fromChangeId " + querySpecification;
        }

        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification);

        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (fromChangeId != null) {
            query.setParameter("fromChangeId", fromChangeId);
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Vyhledání změn na AS.
     *
     * @param fundId identifikátor AS
     * @param nodeId identifikátor JP
     * @param changeId identifikátor změny, od které provádíme vyhledávání (včetně).
     * @return seznam ID změn
     */
    public List<Integer> findChangesAfter(@NotNull Integer fundId, @Nullable Integer nodeId, @Nullable Integer changeId) {

        String selectParams = "ch.change_id";
        String querySpecification = "";

        if (changeId != null) {
            querySpecification = "WHERE ch.change_id >= :changeId " + querySpecification;
        }

        Query query = createFindChangeQuery(selectParams, fundId, nodeId, querySpecification);

        query.setParameter("fundId", fundId);
        if (nodeId != null) {
            query.setParameter("nodeId", nodeId);
        }
        if (changeId != null) {
            query.setParameter("changeId", changeId);
        }

        return ((List<? extends Number>) query.getResultList()).stream().map(Number::intValue).collect(Collectors.toList());
    }

    /**
     * Pomocná struktura změn získaných z DB.
     */
    @SqlResultSetMapping(
            name = "ChangeResultMapping",
            classes = {
                    @ConstructorResult(
                            targetClass = ChangeResult.class,
                            columns = {
                                    @ColumnResult(name = "changeId", type = Integer.class),
                                    @ColumnResult(name = "changeDate", type = OffsetDateTime.class),
                                    @ColumnResult(name = "userId", type = Integer.class),
                                    @ColumnResult(name = "type", type = String.class),
                                    @ColumnResult(name = "primaryNodeId", type = Integer.class),
                                    @ColumnResult(name = "nodeChanges", type = BigInteger.class),
                                    @ColumnResult(name = "weights", type = BigInteger.class),
                            }
                    )
            }
    )
    @Entity
    public static class ChangeResult {

        public ChangeResult() {
        }

        public ChangeResult(final Integer changeId, final OffsetDateTime changeDate, final Integer userId, final String type, final Integer primaryNodeId, final BigInteger nodeChanges, final BigInteger weights) {
            this.changeId = changeId;
            this.changeDate = changeDate;
            this.userId = userId;
            this.type = StringUtils.trim(type);
            this.primaryNodeId = primaryNodeId;
            // pokud je váha (weights) rovna nule, nebyl ovlivněna žádná JP
            this.nodeChanges = ((Number) weights).intValue() == 0 ? BigInteger.ZERO : (BigInteger.valueOf(((Number) nodeChanges).intValue()));
        }

        /**
         * Identifikátor změny.
         */
        @Id
        private Integer changeId;

        /**
         * Datum a čas změny.
         */
        private OffsetDateTime changeDate;

        /**
         * Identifikátor uživatele, který změnu provedl.
         */
        private Integer userId;

        /**
         * Textový zápis typu změny.
         */
        private String type;

        /**
         * Identifikátor JP se kterou změna souvisí.
         */
        private Integer primaryNodeId;

        /**
         * Počet změněných JP způsobené změnou.
         */
        private BigInteger nodeChanges;

        public Integer getChangeId() {
            return changeId;
        }

        public void setChangeId(final Integer changeId) {
            this.changeId = changeId;
        }

        public OffsetDateTime getChangeDate() {
            return changeDate;
        }

        public void setChangeDate(final OffsetDateTime changeDate) {
            this.changeDate = changeDate;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(final Integer userId) {
            this.userId = userId;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public Integer getPrimaryNodeId() {
            return primaryNodeId;
        }

        public void setPrimaryNodeId(final Integer primaryNodeId) {
            this.primaryNodeId = primaryNodeId;
        }

        public BigInteger getNodeChanges() {
            return nodeChanges;
        }

        public void setNodeChanges(final BigInteger nodeChanges) {
            this.nodeChanges = nodeChanges;
        }

    }

}
