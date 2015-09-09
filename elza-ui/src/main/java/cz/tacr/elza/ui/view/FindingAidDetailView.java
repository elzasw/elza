package cz.tacr.elza.ui.view;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.ui.components.Callback;
import ru.xpoft.vaadin.VaadinView;

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
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.ArrFaLevelWithExtraNode;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.ui.ElzaView;
import cz.tacr.elza.ui.components.LevelInlineDetail;
import cz.tacr.elza.ui.components.TreeTable;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;
import cz.tacr.elza.ui.utils.ElzaNotifications;
import cz.tacr.elza.ui.window.DescItemTypeWindow;
import cz.tacr.elza.ui.window.LevelHistoryWindow;
import cz.tacr.elza.ui.window.PosAction;


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


    private Integer findingAidId;
    private ArrFaVersion version;
    private ArrFaLevel rootNode;

    private ArrFindingAid findingAid;
    private ArrFaLevel levelNodeVyjmout;

    AxContainer<ArrArrangementType> arTypeContainer;
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

        rootNode = version.getRootFaLevel();
        HierarchicalCollapsibleBeanItemContainer container = new HierarchicalCollapsibleBeanItemContainer(null, rootNode.getNode().getNodeId());


        List<RulDescItemType> sloupce = new LinkedList<>();
        if (version != null) {
             FaViewDescItemTypes faViewDescItemTypes = ruleSetManager.getFaViewDescItemTypes(version.getFaVersionId());
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
//                ArrFaLevel node = (ArrFaLevel) itemId;
//                return node.getNodeId();
//            }
//        });
        sloupceId.add(POSITION_FIELD);
        table.setColumnHeader(POSITION_FIELD, LEVEL_POSITION);
        table.addGeneratedColumn(POSITION_FIELD, new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrFaLevel node = (ArrFaLevel) itemId;
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
                    ArrFaLevel node = (ArrFaLevel) itemId;
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
                ArrFaLevel node = (ArrFaLevel) event.getItemId();
                ArrFaLevelExt level = arrangementManager.getLevel(node.getNode().getNodeId(), version.getFaVersionId(), null);
                levelDetailConteiner.showLevelDetail(level, level.getDescItemList(), version.getFaVersionId(), creteAttributeEditCallback());
            }
        });

        table.setItemDescriptionGenerator( new Table.ItemDescriptionGenerator() {
            @Override
            public String generateDescription(com.vaadin.ui.Component source, Object itemId,
                    Object propertyId) {
                if(propertyId == null || itemId == null){
                    return null;
                }

                ArrFaLevel node = (ArrFaLevel) itemId;

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
                ArrFaLevel itemId = (ArrFaLevel) collapseEvent.getItemId();
                removeAllChildren(table, itemId, 0);
            }
        });

        table.addExpandListener(new Tree.ExpandListener() {

            @Override
            public void nodeExpand(final Tree.ExpandEvent expandEvent) {
                ArrFaLevel itemId = (ArrFaLevel) expandEvent.getItemId();

                ArrFaLevel itemIdLast = itemId;

                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(itemId.getNode().getNodeId(), version.getFaVersionId());
                for (ArrFaLevel faLevel : faLevels) {
                    if (container.containsId(faLevel)) {
                        break;
                    }
                    BeanItem item = (BeanItem) table.addItemAfter(itemIdLast, faLevel);
                    itemIdLast = faLevel;
                    initNewItemInContainer(item, faLevel, container);
                }
                List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(itemId.getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
                for (ArrFaLevelExt faLevel : faLevelsExt) {
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
    }

    private String getAttributeValue(final Integer nodeId, final Integer position) {
        Map<Integer, String> attributs = attributeCache.get(nodeId);
        if (attributs == null) {
            return "-";
        }

        return StringUtils.defaultString(attributs.get(position));
    }

    private void showDetailAP() {
        table.select(null);
        ArrFaLevelExt level = arrangementManager.getLevel(rootNode.getNode().getNodeId(), version.getFaVersionId(), null);
        levelDetailConteiner.showLevelDetail(level, level.getDescItemList(), version.getFaVersionId(),null);
    }

    private Callback<ArrFaLevelExt> creteAttributeEditCallback() {
        return item -> {

            BeanItem beanItem = (BeanItem) table.getItem(item);

            ArrFaLevel level = (ArrFaLevel) beanItem.getBean();
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

                    ArrFaLevel node = (ArrFaLevel) itemId;
                    ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                    faLevelWithExtraNode.setFaLevel(node);
                    faLevelWithExtraNode.setExtraNode(node.getParentNode());
                    faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());
                    ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelBefore(faLevelWithExtraNode);

                    refreshTree(container, version.getRootFaLevel());

                    /*ArrFaLevel level = faLevelWithExtraNodeRet.getFaLevel();
                    node.setParentNode(faLevelWithExtraNodeRet.getExtraNode());

                    BeanItem item = (BeanItem) container.addItemAt(container.indexOfId(node), level);

                    initNewItemInContainer(item, level, container);
                    repositionLowerSiblings(level, level.getPosition() + 1 , container);

                    refreshParentReferences(node, faLevelWithExtraNodeRet, container);

                    table.refreshRowCache();*/
                    ElzaNotifications.show("Přidáno...");
                }
            ).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat záznam za").icon(FontAwesome.PLUS).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        ArrFaLevel node = (ArrFaLevel) itemId;
                        ArrFaLevel lastId = getItemIdAfterChilds(node, container);

                        ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                        faLevelWithExtraNode.setFaLevel(node);
                        faLevelWithExtraNode.setExtraNode(node.getParentNode());
                        faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());

                        ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelAfter(faLevelWithExtraNode);
                        refreshTree(container, version.getRootFaLevel());

                        /*
                        ArrFaLevel newFaLevel = faLevelWithExtraNodeRet.getFaLevel();
                        node.setParentNode(faLevelWithExtraNodeRet.getExtraNode());

                        repositionLowerSiblings(node, node.getPosition() + 2, container);

                        addItemAfterToContainer(newFaLevel, container, lastId);

                        refreshParentReferences(node, faLevelWithExtraNodeRet, container);


                        table.refreshRowCache();*/

                        ElzaNotifications.show("Přidáno...");
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat podřízený záznam").icon(FontAwesome.PLUS).run(() -> {
                    discardNodeCut();
                    if (table.isCollapsed(itemId)) {
                        table.setCollapsed(itemId, false);
                    }

                    ArrFaLevel itemIdLast = (ArrFaLevel) itemId;
                    ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                    faLevelWithExtraNode.setFaLevel(itemIdLast);
                    ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.addLevelChild(faLevelWithExtraNode);
                    ArrFaLevel newFaLevel = faLevelWithExtraNodeRet.getFaLevel();

                    Collection<?> children = container.getChildren(itemIdLast);
                    if (!CollectionUtils.isEmpty(children)) {
                        Iterator<?> iterator = children.iterator();
                        while (iterator.hasNext()) {
                            itemIdLast = (ArrFaLevel) iterator.next();
                        }
                    }

                    BeanItem item = (BeanItem) table.addItemAfter(itemIdLast, newFaLevel);
                    itemIdLast = newFaLevel;
                    initNewItemInContainer(item, newFaLevel, container);

                    refreshParentReferences(itemIdLast, faLevelWithExtraNodeRet, container);

                    ElzaNotifications.show("Přidáno...");
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Smazat").icon(FontAwesome.TRASH_O).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        ArrFaLevel node = (ArrFaLevel) itemId;
                        ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                        faLevelWithExtraNode.setFaLevel(node);
                        faLevelWithExtraNode.setExtraNode(node.getParentNode());
                        faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());

                        ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.deleteLevel(faLevelWithExtraNode);

                        refreshTree(container, version.getRootFaLevel());

                        /*ArrFaLevel parentNode = (ArrFaLevel) container.getParent(node);
                        if (parentNode == null) {
                            version.getRootFaLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());
                        } else {
                            parentNode.setNode(faLevelWithExtraNodeRet.getExtraNode());
                        }
                        List<ArrFaLevel> childs = container.getChildren(parentNode);
                        for (ArrFaLevel faLevel : childs) {
                            faLevel.setParentNode(faLevelWithExtraNodeRet.getExtraNode());
                        }

                        repositionLowerSiblings(node, node.getPosition(), container);
                        table.removeItem(itemId);

                        table.refreshRowCache();*/

                        ElzaNotifications.show("Smazáno...");
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Vyjmout").icon(FontAwesome.CUT).run(() -> {
                    cutNode((ArrFaLevel) itemId);
                    ElzaNotifications.show("Ve schránce...");
                }).menuItem(parent);

                child = new AxAction().caption("Vložit před").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {

                            ArrFaLevel node = ((ArrFaLevel) itemId);
                            ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                            faLevelWithExtraNode.setFaLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setFaLevelTarget(node);
                            faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());
                            ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelBefore(
                                    faLevelWithExtraNode);

                            refreshTree(container, version.getRootFaLevel());

                            discardNodeCut();

                            /*ArrFaLevel level = faLevelWithExtraNodeRet.getFaLevel();

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
                            ArrFaLevel node = ((ArrFaLevel) itemId);
                            ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                            faLevelWithExtraNode.setFaLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setFaLevelTarget(node);
                            faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());
                            ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelAfter(
                                    faLevelWithExtraNode);
                            refreshTree(container, version.getRootFaLevel());

                            discardNodeCut();

                            /*ArrFaLevel level = faLevelWithExtraNodeRet.getFaLevel();

                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            repositionLowerSiblings((ArrFaLevel) itemId, ((ArrFaLevel) itemId).getPosition() + 2, container);
                            addItemAfterToContainer(level, container, getItemIdAfterChilds((ArrFaLevel) itemId, container));
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
                            ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                            faLevelWithExtraNode.setFaLevel(levelNodeVyjmout);
                            faLevelWithExtraNode.setExtraNode(((ArrFaLevel) itemId).getNode());
                            ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager.moveLevelUnder(faLevelWithExtraNode);
                            container.setCollapsed(((ArrFaLevel) itemId), false);
                            refreshTree(container, version.getRootFaLevel());

                            discardNodeCut();

                            /*ArrFaLevel level = faLevelWithExtraNodeRet.getFaLevel();

                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            if (container.isCollapsed(itemId)) {
                                List<ArrFaLevelExt> faLevelsExt = arrangementManager
                                        .findSubLevels(((ArrFaLevel) itemId).getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
                                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(((ArrFaLevel) itemId).getNode().getNodeId(), version.getFaVersionId());
                                ArrFaLevel idLast = (ArrFaLevel) itemId;
                                for (ArrFaLevel faLevel : faLevels) {
                                    idLast = addItemAfterToContainer(faLevel, container, idLast);
                                }
                                for (ArrFaLevelExt faLevel : faLevelsExt) {
                                    addAttributeToCache(faLevel);
                                }
                            } else {

                                //najdeme posledního přímého potomka
                                ArrFaLevel itemIdLast = (ArrFaLevel) itemId;
                                Collection<?> children = container.getChildren(itemId);
                                if (!CollectionUtils.isEmpty(children)) {
                                    Iterator<?> iterator = children.iterator();
                                    while (iterator.hasNext()) {
                                        itemIdLast = (ArrFaLevel) iterator.next();
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

                child = createHistoryMenuItem((ArrFaLevel) itemId).menuItem(parent);
                return menuBar;
            }
        });
    }

    // aktualizace všech referencí na společného předka
    private void refreshParentReferences(ArrFaLevel node, ArrFaLevelWithExtraNode faLevelWithExtraNodeRet, HierarchicalCollapsibleBeanItemContainer container) {
        ArrFaLevel parentNode = (ArrFaLevel) container.getParent(node);
        if (parentNode == null) {
            version.getRootFaLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());
        }
        List<ArrFaLevel> childs = container.getChildren(parentNode);
        for (ArrFaLevel faLevel : childs) {
            faLevel.setParentNode(faLevelWithExtraNodeRet.getExtraNode());
        }
    }

    private void addActionHistMenu() {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                createHistoryMenuItem((ArrFaLevel) itemId).menuItem(parent);
                return menuBar;
            }
        });
    }

    private AxAction createHistoryMenuItem(ArrFaLevel faLevel) {
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

    private void refreshTree(final HierarchicalCollapsibleBeanItemContainer container, final ArrFaLevel rootLevel) {
        version = arrangementManager.getFaVersionById(version.getFaVersionId());
        int visibleIndex = table.getCurrentPageFirstItemIndex();

        container.removeAllItems();
        List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(rootLevel.getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
        for (ArrFaLevelExt faLevel : faLevelsExt) {
            addAttributeToCache(faLevel);
        }

        List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(rootLevel.getNode().getNodeId(), version.getFaVersionId());

        for (ArrFaLevel faLevel : faLevels) {
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
    private ArrFaLevel getItemIdAfterChilds(final ArrFaLevel itemId, final HierarchicalCollapsibleBeanItemContainer container) {
        ArrFaLevel lastId = itemId;

        Collection<?> childs = container.getChildren(lastId);
        if (!CollectionUtils.isEmpty(childs)) {
            Iterator<?> iterator = childs.iterator();
            Object child = iterator.next();
            while (iterator.hasNext()) {
                child = iterator.next();
            }
            lastId = getItemIdAfterChilds((ArrFaLevel) child, container);
        }

        return lastId;
    }


    private ArrFaLevel addItemAfterToContainer(final ArrFaLevel level,
            final HierarchicalCollapsibleBeanItemContainer container,
            final ArrFaLevel itemIdBefore) {
        BeanItem item = (BeanItem) container.addItemAfter(itemIdBefore, level);
        initNewItemInContainer(item, level, container);

        ArrFaLevel lastId = level;
        if (!container.isCollapsed(level)) {
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId());
            for (ArrFaLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }

        }
        return lastId;
    }

    private ArrFaLevel addItemBeforeToContainer(final ArrFaLevel level,
            final HierarchicalCollapsibleBeanItemContainer container,
            final ArrFaLevel itemIdAfter) {
        BeanItem item = (BeanItem) container.addItemAt(container.indexOfId(itemIdAfter), level);
        initNewItemInContainer(item, level, container);

        ArrFaLevel lastId = level;
        if (!container.isCollapsed(level)) {
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId());
            for (ArrFaLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }
        }
        return lastId;
    }

    private void addAttributeToCache(final ArrFaLevelExt level) {
        Map<Integer, String> attributeList = createAttributeMap(level);
        attributeCache.put(level.getNode().getNodeId(), attributeList);
    }

    private Map<Integer, String> createAttributeMap(final ArrFaLevelExt level) {
        Map<Integer, String> attributeList = new HashMap<>();
        List<ArrDescItemExt> descItemList = level.getDescItemList();
        for (ArrDescItemExt arrDescItemExt : descItemList) {
            Integer descItemTypeId = arrDescItemExt.getDescItemType().getDescItemTypeId();
            String dataStr = arrDescItemExt.getData();
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

    private void addItemToContainer(final ArrFaLevel level, final HierarchicalCollapsibleBeanItemContainer container) {
        BeanItem item = (BeanItem) container.addItem(level);

        initNewItemInContainer(item, level, container);

        if (!container.isCollapsed(level)) {
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNode().getNodeId(), version.getFaVersionId());
            for (ArrFaLevel faLevel : faLevels) {
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
    private void repositionLowerSiblings(final ArrFaLevel itemId, final Integer position,
            final HierarchicalCollapsibleBeanItemContainer container) {
        Collection<ArrFaLevel> lowerSiblings = container.getLowerSiblings(itemId);

        int index = position;
        for (ArrFaLevel lowerSibling : lowerSiblings) {
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
    private void initNewItemInContainer(final BeanItem<ArrFaLevel> item,
            final ArrFaLevel faLevel,
            final HierarchicalCollapsibleBeanItemContainer container) {
        if (faLevel instanceof ArrFaLevelExt) {
            ArrFaLevelExt faLevelExt = (ArrFaLevelExt) faLevel;
            Map<Integer, String> attributeMap = createAttributeMap(faLevelExt);
            if (attributeMap != null) {
                attributeMap.forEach((k,v) -> item.getItemProperty(k).setValue(v));
            }
        }

        if (faLevel.getParentNode().getNodeId().equals(version.getRootFaLevel().getNode().getNodeId())) {
            //hack kvůli chybě ve vaadin, aby byl vložen prvek do seznamu rootů
            if (faLevel.equals(container.firstItemId())) {
                container.setParent(faLevel, container.lastItemId());
            } else {
                container.setParent(faLevel, container.firstItemId());
            }

            container.setParent(faLevel, null);
        } else if (faLevel.getParentNode() != null) {
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

                        ArrFaLevelWithExtraNode faLevelWithExtraNode = new ArrFaLevelWithExtraNode();
                        faLevelWithExtraNode.setFaLevel(version.getRootFaLevel());
                        faLevelWithExtraNode.setRootNode(version.getRootFaLevel().getNode());
                        ArrFaLevelWithExtraNode faLevelWithExtraNodeRet = arrangementManager
                                .addLevelChild(faLevelWithExtraNode);
                        ArrFaLevel newFaLevel = faLevelWithExtraNodeRet.getFaLevel();
                        version.getRootFaLevel().setNode(faLevelWithExtraNodeRet.getExtraNode());

                        // refresh uzlu - vygenerovaná nová verze (zámky)
                        version.getRootFaLevel().setParentNode(faLevelWithExtraNodeRet.getExtraNode());

                        Item item = table.addItem(newFaLevel);

                        HierarchicalCollapsibleBeanItemContainer container
                                = (HierarchicalCollapsibleBeanItemContainer) table
                                .getContainerDataSource();

                        refreshParentReferences(version.getRootFaLevel(), faLevelWithExtraNodeRet, container);

                        if (newFaLevel.getParentNode() != null) {
                            container.setParent(newFaLevel, rootNode);
                        }
                        container.setChildrenAllowed(newFaLevel, true);
                        container.setCollapsed(newFaLevel, true);
                        container.addBean(newFaLevel);
                        table.refreshRowCache();
                        ElzaNotifications.show("Přidáno...");
                    }).exception(new ConcurrentUpdateExceptionHandler()),
                    new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
                    navigate(VersionListView.class, findingAidId)),
                    new AxAction().caption("Uzavřít verzi").icon(FontAwesome.HISTORY).run(() -> {
                        AxForm<VOApproveVersion> formularApproveVersion = formularApproveVersion();
                        ArrFaVersion version = arrangementManager
                                .getOpenVersionByFindingAidId(findingAid.getFindingAidId());
                        VOApproveVersion appVersion = new VOApproveVersion();
                        appVersion.setArrangementTypeId(version.getArrangementType().getArrangementTypeId());
                        appVersion.setRuleSetId(version.getRuleSet().getRuleSetId());

                        approveVersion(formularApproveVersion, appVersion);
                    }),
                    new AxAction().caption("Zobrazit detail AP").icon(FontAwesome.BOOK).run(() ->
                        showDetailAP()),
                    new AxAction().caption("Výběr sloupců").icon(FontAwesome.COG).run(() ->
                        showDescItemTypeWindow())
                    );
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
    }

    private void cutNode(final ArrFaLevel itemId) {
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

        arTypeContainer = new AxContainer<>(ArrArrangementType.class).supplier(ruleSetManager::getArrangementTypes);
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, ArrArrangementType::getName).required();

        ruleSetContainer = new AxContainer<>(RulRuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RulRuleSet::getName).required();
        return form;
    }

    private LevelHistoryWindow showVersionHistory(final ArrFaLevel faLevel) {
        LevelHistoryWindow window = new LevelHistoryWindow(arrangementManager);
        window.show(faLevel, findingAidId);
        return window;
    }

    private DescItemTypeWindow showDescItemTypeWindow() {
        DescItemTypeWindow window = new DescItemTypeWindow(ruleSetManager);
        ArrFaVersion arrFaVersion = arrangementManager.getFaVersionById(version.getFaVersionId());
        window.show(arrFaVersion, this);
        return window;
    }

    @Override
    public void onCommit() {
        Page.getCurrent().reload();
    }
}

