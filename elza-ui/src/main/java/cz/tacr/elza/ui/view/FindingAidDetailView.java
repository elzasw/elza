package cz.tacr.elza.ui.view;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import cz.req.ax.AxAction;
import cz.req.ax.AxComboBox;
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrLevelWithExtraNode;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.generator.SerialNumberGenerator;
import cz.tacr.elza.generator.UnitIdGenerator;
import cz.tacr.elza.ui.ElzaView;
import cz.tacr.elza.ui.components.Callback;
import cz.tacr.elza.ui.components.LevelInlineDetail;
import cz.tacr.elza.ui.components.TreeTable;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;
import cz.tacr.elza.ui.utils.ElzaNotifications;
import cz.tacr.elza.ui.window.DescItemTypeWindow;
import cz.tacr.elza.ui.window.LevelHistoryWindow;
import cz.tacr.elza.ui.window.PosAction;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.xpoft.vaadin.VaadinView;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Seznam archivnívh pomůcek.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 7. 2015
 */
@Component
@Scope("prototype")
@VaadinView("FindingAidDetail")
public class FindingAidDetailView extends ElzaView implements PosAction {

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private RuleManager ruleSetManager;

    @Autowired
    private SerialNumberGenerator serialNumberGenerator;

    @Autowired
    private UnitIdGenerator unitIdGenerator;

    private Integer findingAidId;
    private ArrFindingAidVersion version;
    private ArrLevel rootNode;

    private ArrFindingAid findingAid;
    private ArrLevel levelNodeVyjmout;

    AxContainer<RulArrangementType> arTypeContainer;
    AxContainer<RulRuleSet> ruleSetContainer;

    private TreeTable table;

    @Autowired
    private LevelInlineDetail levelDetailConteiner;

    public static final String LEVEL = "Úroveň";
    public static final String LEVEL_POSITION = "Pozice";
    public static final String POSITION_FIELD = "position";
    public static final String ACTION = "Akce";

    private Map<Integer, Map<Integer, String>> attributeCache = new HashMap<Integer, Map<Integer, String>>();

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        Integer[] params = getParameterIntegers();
        if (params.length == 0) {
            navigate(FindingAidListView.class);
            return;
        }
        findingAidId = params[0];

        loadVersion(params);

        this.findingAid = arrangementManager.getFindingAid(findingAidId);
        discardNodeCut();


        pageTitle(findingAid.getName());
        addActionsButtons(version.getLockChange() != null);

        rootNode = version.getRootLevel();
        HierarchicalCollapsibleBeanItemContainer container = new HierarchicalCollapsibleBeanItemContainer(null, rootNode.getNode().getNodeId());


        List<RulDescItemType> sloupce = new LinkedList<>();
        if (version != null) {
             FaViewDescItemTypes faViewDescItemTypes = ruleSetManager.getFaViewDescItemTypes(version.getFindingAidVersionId());
             sloupce = faViewDescItemTypes.getDescItemTypes();
        }

        table = new TreeTable();
        table.setSelectable(true);
        table.resizeOnPageResize("detail-table");
        table.addStyleName("detail-table");
        table.setWidth("50%");

        table.setContainerDataSource(container);
        table.setSortEnabled(false);
        table.setPageLength(20);

        List<Object> sloupceId = new LinkedList<>();
//        sloupceId.add("faLevelId");
//        table.setColumnHeader("faLevelId", "Úroveň");
//        table.addGeneratedColumn("faLevelId", new Table.ColumnGenerator() {
//            @Override
//            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
//                ArrLevel node = (ArrLevel) itemId;
//                return node.getNodeId();
//            }
//        });
        sloupceId.add(POSITION_FIELD);
        table.setColumnHeader(POSITION_FIELD, LEVEL_POSITION);
        table.addGeneratedColumn(POSITION_FIELD, new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrLevel node = (ArrLevel) itemId;
                return node.getPosition();
            }
        });
        for (RulDescItemType descItemType : sloupce) {
            sloupceId.add(descItemType.getDescItemTypeId());
            table.setColumnHeader(descItemType.getDescItemTypeId(), "<div title=\"" + descItemType.getName()
                    + "\" >" + descItemType.getShortcut() + "</div>");
            table.addGeneratedColumn(descItemType.getDescItemTypeId(), new Table.ColumnGenerator() {
                @Override
                public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                    ArrLevel node = (ArrLevel) itemId;
                    String value = getAttributeValue(node.getNode().getNodeId(), (Integer) columnId);
                    Label result;
                    if (value == null) {
                        result = null;
                    } else {
                        result = newLabel(value);
                        result.setContentMode(ContentMode.HTML);
                    }

                    return result;
                }
            });
        }
        sloupceId.add(ACTION);

        if (version.getLockChange() == null) {
            addActionMenu(container);
            table.setVisibleColumns(sloupceId.toArray());
        } else {
            addActionHistMenu();
            table.setVisibleColumns(sloupceId.toArray());
            if (version.getFindingAid() == null
                    || (!version.getFindingAid().getFindingAidId().equals(findingAidId))) {
                version = null;
            }
        }

        if (version == null) { // chyba nenalezena verze k FA
            navigate(FindingAidListView.class);
            return;
        }

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(final ItemClickEvent event) {
                ArrLevel node = (ArrLevel) event.getItemId();
                ArrLevelExt level = arrangementManager.getLevel(node.getNode().getNodeId(), version.getFindingAidVersionId(), null);
                levelDetailConteiner.showLevelDetail(level, level.getDescItemList(), version.getFindingAidVersionId(), creteAttributeEditCallback());
            }
        });

        table.setItemDescriptionGenerator( new Table.ItemDescriptionGenerator() {
            @Override
            public String generateDescription(com.vaadin.ui.Component source, Object itemId,
                    Object propertyId) {
                if(propertyId == null || itemId == null){
                    return null;
                }

                ArrLevel node = (ArrLevel) itemId;

                if(propertyId instanceof Integer){
                    return getAttributeValue(node.getNode().getNodeId(), (Integer) propertyId);
                }else{
                    Property property = container.getContainerProperty(itemId, propertyId);
                    if(property == null){
                        return null;
                    }

                    Object value = container.getContainerProperty(itemId, propertyId).getValue();
                    return value == null ? null : value.toString();
                }


//                Property property = container.getItem(itemId).getItemProperty(propertyId);
//
//                return property == null || property.getValue() == null ? null : property.getValue().toString();
            }
        });

        refreshTree(container, rootNode);

        table.addCollapseListener(new Tree.CollapseListener() {
            @Override
            public void nodeCollapse(final Tree.CollapseEvent collapseEvent) {
                ArrLevel itemId = (ArrLevel) collapseEvent.getItemId();
                removeAllChildren(table, itemId, 0);
            }
        });

        table.addExpandListener(new Tree.ExpandListener() {

            @Override
            public void nodeExpand(final Tree.ExpandEvent expandEvent) {
                ArrLevel itemId = (ArrLevel) expandEvent.getItemId();

                if (itemId == null) {
                    return;
                }

                ArrLevel itemIdLast = itemId;

                List<ArrLevel> faLevels = arrangementManager.findSubLevels(itemId.getNode().getNodeId(), version.getFindingAidVersionId());
                for (ArrLevel faLevel : faLevels) {
                    if (container.containsId(faLevel)) {
                        break;
                    }
                    BeanItem item = (BeanItem) table.addItemAfter(itemIdLast, faLevel);
                    itemIdLast = faLevel;
                    initNewItemInContainer(item, faLevel, container);
                }
                List<ArrLevelExt> faLevelsExt = arrangementManager.findSubLevels(itemId.getNode().getNodeId(), version.getFindingAidVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
                for (ArrLevelExt faLevel : faLevelsExt) {
                    addAttributeToCache(faLevel);
                }
            }
        });

        if (version != null && version.getLockChange() != null && version.getLockChange().getChangeDate() != null) {
            String createDataStr = version.getLockChange().getChangeDate()
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            com.vaadin.ui.Component verzeComponent = newLabel("Prohlížíte si uzavřenou verzi k " + createDataStr, "h2");
            verzeComponent.setWidth("100%");
            components(verzeComponent, table, levelDetailConteiner);
        } else {
            components(table, levelDetailConteiner);
        }
        showDetailAP();
    }

    private void loadVersion(Integer[] params) {
        Integer versionId;
        if (params.length > 1) {
            versionId = params[1];
        } else {
            versionId = null;
        }

        if (versionId == null) {
            version = arrangementManager.getOpenVersionByFindingAidId(findingAidId);
        } else {
            version = arrangementManager.getFaVersionById(versionId);
        }

        fixVersionSessionClient();
    }

    private void fixVersionSessionClient() {
        ArrFindingAid findingAidFix = version.getFindingAid();
        findingAidFix.getFindingAidId();
    }

    private String getAttributeValue(final Integer nodeId, final Integer position) {
        Map<Integer, String> attributs = attributeCache.get(nodeId);
        if (attributs == null) {
            return "";
        }

        return StringUtils.defaultString(attributs.get(position));
    }

    private void showDetailAP() {
        table.select(null);
        ArrLevelExt level = arrangementManager.getLevel(rootNode.getNode().getNodeId(), version.getFindingAidVersionId(), null);
        levelDetailConteiner.showLevelDetail(level, level.getDescItemList(), version.getFindingAidVersionId(),null);
        levelDetailConteiner.showNodeRegisterLink(version, level.getNode());
    }

    private Callback<ArrLevelExt> creteAttributeEditCallback() {
        return item -> {

            BeanItem beanItem = (BeanItem) table.getItem(item);

            ArrLevel level = (ArrLevel) beanItem.getBean();
            level.setNode(item.getNode());

            addAttributeToCache(item);
            table.refreshRowCache();
        };
    }


    private void addActionMenu(HierarchicalCollapsibleBeanItemContainer container) {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                MenuBar.MenuItem child = new AxAction().caption("Přidat záznam před").icon(FontAwesome.PLUS).run(() -> {
                    discardNodeCut();

                    ArrLevel node = (ArrLevel) itemId;
                    ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                    faLevelWithExtraNode.setLevel(node);
                    faLevelWithExtraNode.setExtraNode(node.getNodeParent());
                    faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                    faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                    try {
                        ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelBefore(faLevelWithExtraNode);

                        refreshTree(container, version.getRootLevel());

                        /*ArrLevel level = faLevelWithExtraNodeRet.getLevel();
                        node.setNodeParent(faLevelWithExtraNodeRet.getExtraNode());

                        BeanItem item = (BeanItem) container.addItemAt(container.indexOfId(node), level);

                        initNewItemInContainer(item, level, container);
                        repositionLowerSiblings(level, level.getPosition() + 1 , container);

                        refreshParentReferences(node, faLevelWithExtraNodeRet, container);

                        table.refreshRowCache();*/
                        ElzaNotifications.show("Přidáno...");
                    }
                    catch (IllegalStateException | IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }
            ).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat záznam za").icon(FontAwesome.PLUS).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        ArrLevel node = (ArrLevel) itemId;
                        ArrLevel lastId = getItemIdAfterChilds(node, container);

                        ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                        faLevelWithExtraNode.setLevel(node);
                        faLevelWithExtraNode.setExtraNode(node.getNodeParent());
                        faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                        faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                        try {
                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelAfter(faLevelWithExtraNode);
                            refreshTree(container, version.getRootLevel());

                            /*
                            ArrLevel newFaLevel = faLevelWithExtraNodeRet.getLevel();
                            node.setNodeParent(faLevelWithExtraNodeRet.getExtraNode());

                            repositionLowerSiblings(node, node.getPosition() + 2, container);

                            addItemAfterToContainer(newFaLevel, container, lastId);

                            refreshParentReferences(node, faLevelWithExtraNodeRet, container);


                            table.refreshRowCache();*/

                            ElzaNotifications.show("Přidáno...");
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            ElzaNotifications.showError(e.getMessage());
                        }
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat podřízený záznam").icon(FontAwesome.PLUS).run(() -> {
                    discardNodeCut();
                    if (table.isCollapsed(itemId)) {
                        table.setCollapsed(itemId, false);
                    }

                    ArrLevel itemIdLast = (ArrLevel) itemId;
                    ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                    faLevelWithExtraNode.setLevel(itemIdLast);
                    faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                    try {
                        ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelChild(faLevelWithExtraNode);
                        ArrLevel newFaLevel = faLevelWithExtraNodeRet.getLevel();

                        Collection<?> children = container.getChildren(itemIdLast);
                        if (!CollectionUtils.isEmpty(children)) {
                            Iterator<?> iterator = children.iterator();
                            while (iterator.hasNext()) {
                                itemIdLast = (ArrLevel) iterator.next();
                            }
                        }

                        BeanItem item = (BeanItem) table.addItemAfter(itemIdLast, newFaLevel);
                        itemIdLast = newFaLevel;
                        initNewItemInContainer(item, newFaLevel, container);

                        refreshParentReferences(itemIdLast, faLevelWithExtraNodeRet, container);

                        ElzaNotifications.show("Přidáno...");
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Smazat").icon(FontAwesome.TRASH_O).run(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            discardNodeCut();
                            ArrLevel node = (ArrLevel) itemId;
                            ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                            faLevelWithExtraNode.setLevel(node);
                            faLevelWithExtraNode.setExtraNode(node.getNodeParent());
                            faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                            faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.deleteLevel(faLevelWithExtraNode);

                            refreshTree(container, version.getRootLevel());

                            /*ArrLevel parentNode = (ArrLevel) container.getParent(node);
                            if (parentNode == null) {
                                version.getRootFaLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());
                            } else {
                                parentNode.setNode(faLevelWithExtraNodeRet.getExtraNode());
                            }
                            List<ArrLevel> childs = container.getChildren(parentNode);
                            for (ArrLevel faLevel : childs) {
                                faLevel.setNodeParent(faLevelWithExtraNodeRet.getExtraNode());
                            }

                            repositionLowerSiblings(node, node.getPosition(), container);
                            table.removeItem(itemId);

                            table.refreshRowCache();*/

                            ElzaNotifications.show("Smazáno...");
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            ElzaNotifications.showError(e.getMessage());
                        }
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Vyjmout").icon(FontAwesome.CUT).run(() -> {
                    cutNode((ArrLevel) itemId);
                    ElzaNotifications.show("Ve schránce...");
                }).menuItem(parent);

                child = new AxAction().caption("Vložit před").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {

                            ArrLevel node = ((ArrLevel) itemId);
                            ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                            faLevelWithExtraNode.setLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setLevelTarget(node);
                            faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                            faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelBefore(
                                    faLevelWithExtraNode);

                            refreshTree(container, version.getRootLevel());

                            discardNodeCut();

                            /*ArrLevel level = faLevelWithExtraNodeRet.getLevel();

                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            addItemBeforeToContainer(level, container, node);
                            repositionLowerSiblings(level, level.getPosition() + 1, container);
                            discardNodeCut();
                            table.refreshRowCache();*/

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);
                child.setStyleName("show-if-cut");

                child = new AxAction().caption("Vložit za").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {
                            ArrLevel node = ((ArrLevel) itemId);
                            ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                            faLevelWithExtraNode.setLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setLevelTarget(node);
                            faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                            faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelAfter(
                                    faLevelWithExtraNode);
                            refreshTree(container, version.getRootLevel());

                            discardNodeCut();

                            /*ArrLevel level = faLevelWithExtraNodeRet.getLevel();

                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            repositionLowerSiblings((ArrLevel) itemId, ((ArrLevel) itemId).getPosition() + 2, container);
                            addItemAfterToContainer(level, container, getItemIdAfterChilds((ArrLevel) itemId, container));
                            discardNodeCut();
                            table.refreshRowCache();*/

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);
                child.setStyleName("show-if-cut");


                child = new AxAction().caption("Vložit jako podřízený").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {
                            ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                            faLevelWithExtraNode.setLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setExtraNode(((ArrLevel) itemId).getNode());
                            faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());

                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelUnder(faLevelWithExtraNode);
                            container.setCollapsed(((ArrLevel) itemId), false);
                            refreshTree(container, version.getRootLevel());

                            discardNodeCut();

                            /*ArrLevel level = faLevelWithExtraNodeRet.getLevel();

                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            if (container.isCollapsed(itemId)) {
                                List<ArrLevelExt> faLevelsExt = arrangementManager
                                        .findSubLevels(((ArrLevel) itemId).getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
                                List<ArrLevel> faLevels = arrangementManager.findSubLevels(((ArrLevel) itemId).getNode().getNodeId(), version.getFaVersionId());
                                ArrLevel idLast = (ArrLevel) itemId;
                                for (ArrLevel faLevel : faLevels) {
                                    idLast = addItemAfterToContainer(faLevel, container, idLast);
                                }
                                for (ArrLevelExt faLevel : faLevelsExt) {
                                    addAttributeToCache(faLevel);
                                }
                            } else {

                                //najdeme posledního přímého potomka
                                ArrLevel itemIdLast = (ArrLevel) itemId;
                                Collection<?> children = container.getChildren(itemId);
                                if (!CollectionUtils.isEmpty(children)) {
                                    Iterator<?> iterator = children.iterator();
                                    while (iterator.hasNext()) {
                                        itemIdLast = (ArrLevel) iterator.next();
                                    }
                                }
                                addItemAfterToContainer(level, container, itemIdLast);
                            }

                            discardNodeCut();
                            table.refreshRowCache();*/

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);
                child.setStyleName("show-if-cut");

                child = createHistoryMenuItem((ArrLevel) itemId).menuItem(parent);
                return menuBar;
            }
        });
    }

    // aktualizace všech referencí na společného předka
    private void refreshParentReferences(ArrLevel node, ArrLevelWithExtraNode faLevelWithExtraNodeRet, HierarchicalCollapsibleBeanItemContainer container) {
        ArrLevel parentNode = (ArrLevel) container.getParent(node);
        if (parentNode == null) {
            version.getRootLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());
        }
        List<ArrLevel> childs = container.getChildren(parentNode);
        for (ArrLevel faLevel : childs) {
            faLevel.setNodeParent(faLevelWithExtraNodeRet.getExtraNode());
        }
    }

    private void addActionHistMenu() {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                createHistoryMenuItem((ArrLevel) itemId).menuItem(parent);
                return menuBar;
            }
        });
    }

    private AxAction createHistoryMenuItem(ArrLevel faLevel) {
        return new AxAction().caption("Zobrazit historii").icon(FontAwesome.CALENDAR)
            .run(() -> {
                showVersionHistory(faLevel);
            });
    }

    private boolean checkPaste() {
        if (levelNodeVyjmout == null) {
            ElzaNotifications.show("Není co vložit, nejprve je potřeba vyjmout uzel.");
            return false;
        } else {
            return true;
        }
    }

    private void refreshTree(final HierarchicalCollapsibleBeanItemContainer container, final ArrLevel rootLevel) {
        version = arrangementManager.getFaVersionById(version.getFindingAidVersionId());
        fixVersionSessionClient();

        int visibleIndex = table.getCurrentPageFirstItemIndex();

        container.removeAllItems();
        List<ArrLevelExt> faLevelsExt = arrangementManager.findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
        for (ArrLevelExt faLevel : faLevelsExt) {
            addAttributeToCache(faLevel);
        }

        List<ArrLevel> faLevels = arrangementManager.findSubLevels(version.getRootLevel().getNode().getNodeId(), version.getFindingAidVersionId());

        for (ArrLevel faLevel : faLevels) {
            addItemToContainer(faLevel, container);
        }

        table.setCurrentPageFirstItemIndex(visibleIndex);
    }

    /**
     * Najde posledního potomka v rozbaleném stromu, za kterého vkládáme v seznamu novou položku.
     *
     * @param itemId id položky, za kterou budeme vkládat
     * @return id posledního potomka
     */
    private ArrLevel getItemIdAfterChilds(final ArrLevel itemId, final HierarchicalCollapsibleBeanItemContainer container) {
        ArrLevel lastId = itemId;

        Collection<?> childs = container.getChildren(lastId);
        if (!CollectionUtils.isEmpty(childs)) {
            Iterator<?> iterator = childs.iterator();
            Object child = iterator.next();
            while (iterator.hasNext()) {
                child = iterator.next();
            }
            lastId = getItemIdAfterChilds((ArrLevel) child, container);
        }

        return lastId;
    }


    private ArrLevel addItemAfterToContainer(final ArrLevel level,
            final HierarchicalCollapsibleBeanItemContainer container,
            final ArrLevel itemIdBefore) {
        BeanItem item = (BeanItem) container.addItemAfter(itemIdBefore, level);
        initNewItemInContainer(item, level, container);

        ArrLevel lastId = level;
        if (!container.isCollapsed(level)) {
            List<ArrLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId());
            for (ArrLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }

        }
        return lastId;
    }

    private ArrLevel addItemBeforeToContainer(final ArrLevel level,
            final HierarchicalCollapsibleBeanItemContainer container,
            final ArrLevel itemIdAfter) {
        BeanItem item = (BeanItem) container.addItemAt(container.indexOfId(itemIdAfter), level);
        initNewItemInContainer(item, level, container);

        ArrLevel lastId = level;
        if (!container.isCollapsed(level)) {
            List<ArrLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId());
            for (ArrLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }
        }
        return lastId;
    }

    private void addAttributeToCache(final ArrLevelExt level) {
        Map<Integer, String> attributeList = createAttributeMap(level);
        attributeCache.put(level.getNode().getNodeId(), attributeList);
    }

    private Map<Integer, String> createAttributeMap(final ArrLevelExt level) {
        Map<Integer, String> attributeList = new HashMap<>();
        List<ArrDescItem> descItemList = level.getDescItemList();
        for (ArrDescItem arrDescItemExt : descItemList) {
            Integer descItemTypeId = arrDescItemExt.getDescItemType().getDescItemTypeId();
            String dataStr = arrDescItemExt.toString();
            if (arrDescItemExt.getDescItemSpec() != null) {
                String specShortcut = arrDescItemExt.getDescItemSpec().getShortcut();
                dataStr = specShortcut + "(" + StringUtils.defaultString(dataStr) + ")";
            }
            String attribute = attributeList.get(descItemTypeId);
            if (attribute == null) {
                attribute = dataStr;
            } else {
                attribute += ", " + dataStr;
            }
            attributeList.put(descItemTypeId, attribute);
        }
        return attributeList;
    }

    private void addItemToContainer(final ArrLevel level, final HierarchicalCollapsibleBeanItemContainer container) {
        BeanItem item = (BeanItem) container.addItem(level);

        initNewItemInContainer(item, level, container);

        if (!container.isCollapsed(level)) {
            List<ArrLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFindingAidVersionId());
            for (ArrLevel faLevel : faLevels) {
                addItemToContainer(faLevel, container);
            }
        }
    }

    /**
     * Provede přečíslování sourozenců pod daným id.
     *
     * @param itemId   id
     * @param position pozice prvního sourozence pod id
     */
    private void repositionLowerSiblings(final ArrLevel itemId, final Integer position,
            final HierarchicalCollapsibleBeanItemContainer container) {
        Collection<ArrLevel> lowerSiblings = container.getLowerSiblings(itemId);

        int index = position;
        for (ArrLevel lowerSibling : lowerSiblings) {
            lowerSibling.setPosition(index++);
        }
    }


    /**
     * Nastaví novou položku v konejneru.
     *
     * @param item      nová položka
     * @param faLevel   level
     * @param container kontejner
     */
    private void initNewItemInContainer(final BeanItem<ArrLevel> item,
            final ArrLevel faLevel,
            final HierarchicalCollapsibleBeanItemContainer container) {
        if (faLevel instanceof ArrLevelExt) {
            ArrLevelExt faLevelExt = (ArrLevelExt) faLevel;
            Map<Integer, String> attributeMap = createAttributeMap(faLevelExt);
            if (attributeMap != null) {
                attributeMap.forEach((k,v) -> item.getItemProperty(k).setValue(v));
            }
        }

        if (faLevel.getNodeParent().getNodeId().equals(version.getRootLevel().getNode().getNodeId())) {
            //hack kvůli chybě ve vaadin, aby byl vložen prvek do seznamu rootů
            if (faLevel.equals(container.firstItemId())) {
                container.setParent(faLevel, container.lastItemId());
            } else {
                container.setParent(faLevel, container.firstItemId());
            }

            container.setParent(faLevel, null);
        } else if (faLevel.getNodeParent() != null) {
            container.setParent(faLevel, item.getBean());
        }

        container.addBean(faLevel);
        container.setChildrenAllowed(faLevel, true);
        container.setCollapsed(faLevel, container.isCollapsed(faLevel));
    }

    private void removeAllChildren(final TreeTable table, final Object itemId, final int level) {
        Collection<?> children = table.getChildren(itemId);
        if (children != null) {
            ArrayList tmpChildren = new ArrayList(children);
            Iterator<?> itChilder = tmpChildren.iterator();
            while (itChilder.hasNext()) {
                removeAllChildren(table, itChilder.next(), level + 1);
//                table.removeItem(itChilder.next());
            }
        }


        table.setCollapsed(itemId, true);
        if(level > 0){
            table.removeItem(itemId);
        }
    }



    private void addActionsButtons(boolean historiOnly) {
        if (historiOnly) {
            AxAction hist = new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
            navigate(VersionListView.class, findingAidId));
            AxAction detail = new AxAction().caption("Zobrazit detail AP").icon(FontAwesome.BOOK).run(() ->
            showDetailAP());
            AxAction itemTypes = new AxAction().caption("Výběr sloupců").icon(FontAwesome.COG).run(() ->
                     showDescItemTypeWindow());
            actions(hist, detail, itemTypes);
        } else {
            actions(
                    new AxAction().caption("Přidat záznam").icon(FontAwesome.PLUS).run(() -> {

                        ArrLevelWithExtraNode faLevelWithExtraNode = new ArrLevelWithExtraNode();
                        faLevelWithExtraNode.setLevel(version.getRootLevel());
                        faLevelWithExtraNode.setRootNode(version.getRootLevel().getNode());
                        faLevelWithExtraNode.setFaVersionId(version.getFindingAidVersionId());
                        try {
                            ArrLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager
                                    .addLevelChild(faLevelWithExtraNode);
                            ArrLevel newFaLevel = faLevelWithExtraNodeRet.getLevel();
                            version.getRootLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());

                            // refresh uzlu - vygenerovaná nová verze (zámky)
                            version.getRootLevel().setNodeParent(faLevelWithExtraNodeRet.getExtraNode());

                            Item item = table.addItem(newFaLevel);

                            HierarchicalCollapsibleBeanItemContainer container
                                    = (HierarchicalCollapsibleBeanItemContainer) table
                                    .getContainerDataSource();

                            refreshParentReferences(version.getRootLevel(), faLevelWithExtraNodeRet, container);

                            if (newFaLevel.getNodeParent() != null) {
                                container.setParent(newFaLevel, rootNode);
                            }
                            container.setChildrenAllowed(newFaLevel, true);
                            container.setCollapsed(newFaLevel, true);
                            container.addBean(newFaLevel);
                            table.refreshRowCache();
                            ElzaNotifications.show("Přidáno...");
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            ElzaNotifications.showError(e.getMessage());
                        }
                    }).exception(new ConcurrentUpdateExceptionHandler()),
                    new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
                            navigate(VersionListView.class, findingAidId)),
                    new AxAction().caption("Uzavřít verzi").icon(FontAwesome.HISTORY).run(() -> {
                        AxForm<VOApproveVersion> formularApproveVersion = formularApproveVersion();
                        ArrFindingAidVersion version = arrangementManager
                                .getOpenVersionByFindingAidId(findingAid.getFindingAidId());
                        VOApproveVersion appVersion = new VOApproveVersion();
                        appVersion.setArrangementTypeId(version.getArrangementType().getArrangementTypeId());
                        appVersion.setRuleSetId(version.getRuleSet().getRuleSetId());

                        approveVersion(formularApproveVersion, appVersion);
                    }),
                    new AxAction().caption("Zobrazit detail AP").icon(FontAwesome.BOOK).run(() ->
                        showDetailAP()),
                    new AxAction().caption("Výběr sloupců").icon(FontAwesome.COG).run(() ->
                        showDescItemTypeWindow()),
                    new AxAction().caption("Gen REBUILD").icon(FontAwesome.PLUS_CIRCLE).run(() -> {
                        serialNumberGenerator.rebuild(version);
                        unitIdGenerator.rebuild(version);

                        HierarchicalCollapsibleBeanItemContainer container = (HierarchicalCollapsibleBeanItemContainer) table.getContainerDataSource();
                        refreshTree(container, version.getRootLevel());
                    }),
                    new AxAction().caption("Gen CLEAN").icon(FontAwesome.TRASH_O).run(() -> {
                        serialNumberGenerator.clean(version);
                        unitIdGenerator.clean(version);

                        HierarchicalCollapsibleBeanItemContainer container = (HierarchicalCollapsibleBeanItemContainer) table.getContainerDataSource();
                        refreshTree(container, version.getRootLevel());
                    }));
        }
    }

    private void approveVersion(final AxForm<VOApproveVersion> form, final VOApproveVersion appVersion) {
        form.setValue(appVersion);
        new AxWindow().caption("Uzavření verze archivní pomůcky").components(form)
        .buttonClose().buttonPrimary(new AxAction<VOApproveVersion>()
                .caption("Uložit")
                .exception(new ConcurrentUpdateExceptionHandler())
                .primary()
                .value(form::commit)
                .action(this::approveVersion)
                .runAfter(() -> navigate(FindingAidDetailView.class, findingAid.getFindingAidId()))
                ).modal().style("fa-window-detail").show();

    }

    private void approveVersion(final VOApproveVersion voApproveVersion) {
        version = arrangementManager.approveVersion(version, voApproveVersion.getArrangementTypeId(),
                voApproveVersion.getRuleSetId());
        fixVersionSessionClient();
    }

    private void cutNode(final ArrLevel itemId) {
        UI.getCurrent().setOverlayContainerLabel("");
        levelNodeVyjmout = itemId;
    }

    private void discardNodeCut() {
        UI.getCurrent().setOverlayContainerLabel("item-not-cut");
        levelNodeVyjmout = null;
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularApproveVersion")
    private AxForm<VOApproveVersion> formularApproveVersion() {
        AxForm<VOApproveVersion> form = AxForm.init(VOApproveVersion.class);
        form.addStyleName("fa-form");

        ruleSetContainer = new AxContainer<>(RulRuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        AxForm<AxComboBox>.AxField<AxComboBox> ruleSetCombo = form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RulRuleSet::getName).required();
        ruleSetCombo.field().addValueChangeListener((event) -> {
            arTypeContainer.refresh();
        });

        arTypeContainer = new AxContainer<>(RulArrangementType.class).supplier((repository) -> {
            Integer ruleSetId = (Integer) ruleSetCombo.field().getValue();
            if (ruleSetId == null) {
                return new ArrayList<RulArrangementType>();
            } else {
                return ruleSetManager.getArrangementTypes(ruleSetId);
            }
        });
        arTypeContainer.addAll(new ArrayList<RulArrangementType>());
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, RulArrangementType::getName).required();

        return form;
    }

    private LevelHistoryWindow showVersionHistory(final ArrLevel faLevel) {
        LevelHistoryWindow window = new LevelHistoryWindow(arrangementManager);
        window.show(faLevel, findingAidId);
        return window;
    }

    private DescItemTypeWindow showDescItemTypeWindow() {
        DescItemTypeWindow window = new DescItemTypeWindow(ruleSetManager);
        ArrFindingAidVersion arrFaVersion = arrangementManager.getFaVersionById(version.getFindingAidVersionId());
        window.show(arrFaVersion, this);
        return window;
    }

    @Override
    public void onCommit() {
        Page.getCurrent().reload();
    }
}

