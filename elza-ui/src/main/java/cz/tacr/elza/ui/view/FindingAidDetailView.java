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

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;
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
import cz.tacr.elza.ui.ElzaView;
import cz.tacr.elza.ui.components.LevelInlineDetail;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;
import cz.tacr.elza.ui.utils.ElzaNotifications;
import cz.tacr.elza.ui.window.DescItemTypeWindow;
import cz.tacr.elza.ui.window.LevelHistoryWindow;


/**
 * Seznam archivnívh pomůcek.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 7. 2015
 */
@Component
@Scope("prototype")
@VaadinView("FindingAidDetail")
public class FindingAidDetailView extends ElzaView {

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

        rootNode = arrangementManager.findLevelByNodeId(version.getRootNode().getNodeId() , version.getFaVersionId());
        HierarchicalCollapsibleBeanItemContainer container = new HierarchicalCollapsibleBeanItemContainer(null, rootNode.getNodeId());


        List<RulDescItemType> sloupce = new LinkedList<>();
        if (version != null) {
            sloupce = ruleSetManager.getFaViewDescItemTypes(version.getFaVersionId());
        }

        table = new TreeTable();
        table.addStyleName("detail-table");
        table.setWidth("50%");

        table.setContainerDataSource(container);
        table.setSortEnabled(false);
        table.setPageLength(20);

        List<Object> sloupceId = new LinkedList<>();
        sloupceId.add("faLevelId");
        table.setColumnHeader("faLevelId", "Úroveň");
        table.addGeneratedColumn("faLevelId", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrFaLevel node = (ArrFaLevel) itemId;
                return node.getNodeId();
            }
        });
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
            table.setColumnHeader(descItemType.getDescItemTypeId(), descItemType.getShortcut());
            table.addGeneratedColumn(descItemType.getDescItemTypeId(), new Table.ColumnGenerator() {
                @Override
                public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                    ArrFaLevel node = (ArrFaLevel) itemId;
                    return getAttributeValue(node.getNodeId(), (Integer)columnId);
                }
            });
        }
        sloupceId.add(ACTION);

        if (version.getLockChange() == null) {
            addActionMenu(container);
            table.setVisibleColumns(sloupceId.toArray());
        } else {
            addActionHistMenu(container);
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
                table.setWidth("50%");
                ArrFaLevel node = (ArrFaLevel) event.getItemId();
                ArrFaLevelExt level = arrangementManager.getLevel(node.getNodeId(), version.getFaVersionId(), null);
                levelDetailConteiner.showLevelDetail(level, level.getDescItemList());
            }
        });

        refreshTree(container, rootNode);

        table.addCollapseListener(new Tree.CollapseListener() {
            @Override
            public void nodeCollapse(final Tree.CollapseEvent collapseEvent) {
                ArrFaLevel itemId = (ArrFaLevel) collapseEvent.getItemId();
                removeAllChildren(table, itemId);
            }
        });

        table.addExpandListener(new Tree.ExpandListener() {

            @Override
            public void nodeExpand(final Tree.ExpandEvent expandEvent) {
                ArrFaLevel itemId = (ArrFaLevel) expandEvent.getItemId();

                ArrFaLevel itemIdLast = itemId;

                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(itemId.getNodeId(), version.getFaVersionId());
                for (ArrFaLevel faLevel : faLevels) {
                    if (container.containsId(faLevel)) {
                        break;
                    }
                    BeanItem item = (BeanItem) table.addItemAfter(itemIdLast, faLevel);
                    itemIdLast = faLevel;
                    initNewItemInContainer(item, faLevel, container);
                }
                List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(itemId.getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
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
            components(verzeComponent, table, createInlineDetail());
        } else {
            components(table, createInlineDetail());
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
        table.setWidth("50%");
        ArrFaLevelExt level = arrangementManager.getLevel(rootNode.getNodeId(), version.getFaVersionId(), null);
        levelDetailConteiner.showLevelDetail(level, level.getDescItemList());
    }

    private CssLayout createInlineDetail() {
        levelDetailConteiner = new LevelInlineDetail(new Runnable() {
            @Override
            public void run() {
                table.setWidth("100%");
            }
        });

        return levelDetailConteiner;
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
                    ArrFaLevel level = arrangementManager.addLevelBefore(node);
                    BeanItem item = (BeanItem) container.addItemAt(container.indexOfId(node), level);

                    initNewItemInContainer(item, level, container);
                    repositionLowerSiblings(level, level.getPosition() + 1 , container);
                    table.refreshRowCache();
                    ElzaNotifications.show("Přidáno...");
                }
            ).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat záznam za").icon(FontAwesome.PLUS).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        ArrFaLevel node = (ArrFaLevel) itemId;
                        ArrFaLevel lastId = getItemIdAfterChilds(node, container);

                        ArrFaLevel newFaLevel = arrangementManager.addLevelAfter(node);

                        repositionLowerSiblings(node, node.getPosition() + 2, container);

                        addItemAfterToContainer(newFaLevel, container, lastId);
                        table.refreshRowCache();

                        ElzaNotifications.show("Přidáno...");
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Přidat podřízený záznam").icon(FontAwesome.PLUS).run(() -> {
                    discardNodeCut();
                    if (table.isCollapsed(itemId)) {
                        table.setCollapsed(itemId, false);
                    }

                    ArrFaLevel itemIdLast = (ArrFaLevel) itemId;
                    ArrFaLevel newFaLevel = arrangementManager.addLevelChild(itemIdLast);

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
                    ElzaNotifications.show("Přidáno...");
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);

                child = new AxAction().caption("Smazat").icon(FontAwesome.TRASH_O).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        ArrFaLevel node = (ArrFaLevel) itemId;
                        arrangementManager.deleteLevel(node.getNodeId());

                        repositionLowerSiblings(node, node.getPosition(), container);

                        table.removeItem(itemId);
                        table.refreshRowCache();
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
                            ArrFaLevel level = arrangementManager.moveLevelBefore(levelNodeVyjmout, ((ArrFaLevel) itemId).getNodeId());
                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            addItemBeforeToContainer(level, container, (ArrFaLevel) itemId);
                            repositionLowerSiblings(level, level.getPosition() + 1, container);
                            discardNodeCut();
                            table.refreshRowCache();

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
                            ArrFaLevel level = arrangementManager.moveLevelAfter(levelNodeVyjmout, ((ArrFaLevel) itemId).getNodeId());
                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            repositionLowerSiblings((ArrFaLevel) itemId, ((ArrFaLevel) itemId).getPosition() + 2, container);
                            addItemAfterToContainer(level, container, getItemIdAfterChilds((ArrFaLevel) itemId, container));
                            discardNodeCut();
                            table.refreshRowCache();

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
                            ArrFaLevel level = arrangementManager.moveLevelUnder(levelNodeVyjmout, ((ArrFaLevel) itemId).getNodeId());
                            repositionLowerSiblings(levelNodeVyjmout, levelNodeVyjmout.getPosition(), container);
                            table.removeItem(levelNodeVyjmout);

                            if (container.isCollapsed(itemId)) {
                                List<ArrFaLevelExt> faLevelsExt = arrangementManager
                                        .findSubLevels(((ArrFaLevel) itemId).getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
                                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(((ArrFaLevel) itemId).getNodeId(), version.getFaVersionId());
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
                            table.refreshRowCache();

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).exception(new ConcurrentUpdateExceptionHandler()).menuItem(parent);
                child.setStyleName("show-if-cut");

                child = new AxAction().caption("Zobrazit historii").icon(FontAwesome.CALENDAR).run(() -> {
                    showVersionHistory((Integer) itemId);
                }).menuItem(parent);
                return menuBar;
            }
        });
    }

    private void addActionHistMenu(HierarchicalCollapsibleBeanItemContainer container) {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                MenuBar.MenuItem child = new AxAction().caption("Zobrazit historii").icon(FontAwesome.CALENDAR)
                        .run(() -> {
                            showVersionHistory((Integer) itemId);
                        }).menuItem(parent);
                return menuBar;
            }
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
        container.removeAllItems();
        List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(rootLevel.getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
        for (ArrFaLevelExt faLevel : faLevelsExt) {
            addAttributeToCache(faLevel);
        }

        List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(rootLevel.getNodeId(), version.getFaVersionId());

        for (ArrFaLevel faLevel : faLevels) {
            addItemToContainer(faLevel, container);
        }
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
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId());
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
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId());
            for (ArrFaLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }
        }
        return lastId;
    }

    private void addAttributeToCache(final ArrFaLevelExt level) {
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
        attributeCache.put(level.getNodeId(), attributeList);
    }

    private void addItemToContainer(final ArrFaLevel level, final HierarchicalCollapsibleBeanItemContainer container) {
        BeanItem item = (BeanItem) container.addItem(level);

        initNewItemInContainer(item, level, container);

        if (!container.isCollapsed(level)) {
            List<ArrFaLevelExt> faLevelsExt = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId(), ArrangementManager.FORMAT_ATTRIBUTE_SHORT, null);
            for (ArrFaLevelExt faLevel : faLevelsExt) {
                addAttributeToCache(faLevel);
            }

            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), version.getFaVersionId());
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
        if (faLevel.getParentNodeId().equals(version.getRootNode().getNodeId())) {
            //hack kvůli chybě ve vaadin, aby byl vložen prvek do seznamu rootů
            if (faLevel.equals(container.firstItemId())) {
                container.setParent(faLevel, container.lastItemId());
            } else {
                container.setParent(faLevel, container.firstItemId());
            }

            container.setParent(faLevel, null);
        } else if (faLevel.getParentNodeId() != null) {
            container.setParent(faLevel, item.getBean());
        }

        container.addBean(faLevel);
        container.setChildrenAllowed(faLevel, true);
        container.setCollapsed(faLevel, container.isCollapsed(faLevel));
    }

    private void removeAllChildren(final TreeTable table, final ArrFaLevel itemId) {
        Collection<?> children = table.getChildren(itemId);
        if (children != null) {
            ArrayList tmpChildren = new ArrayList(children);
            Iterator<?> itChilder = tmpChildren.iterator();
            while (itChilder.hasNext()) {
                table.removeItem(itChilder.next());
            }
        }
    }

    private void addActionsButtons(boolean historiOnly) {
        if (historiOnly) {
            AxAction hist = new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
            navigate(VersionListView.class, findingAidId));
            AxAction detail = new AxAction().caption("Zobrazit detail AP").icon(FontAwesome.BOOK).run(() ->
            showDetailAP());
            AxAction itemTypes = new AxAction().caption("Výběr sloupců").icon(FontAwesome.BOOK).run(() ->
                     showDescItemTypeWindow());
            actions(hist, detail, itemTypes);
        } else {
            actions(
                    new AxAction().caption("Přidat záznam").icon(FontAwesome.PLUS).run(() -> {

                        ArrFaLevel newFaLevel = arrangementManager.addLevel(findingAidId);

                        Item item = table.addItem(newFaLevel);

                        HierarchicalCollapsibleBeanItemContainer container = (HierarchicalCollapsibleBeanItemContainer) table
                                .getContainerDataSource();

                        if (newFaLevel.getParentNodeId() != null) {
                            container.setParent(newFaLevel, rootNode);
                        }
                        container.setChildrenAllowed(newFaLevel, true);
                        container.setCollapsed(newFaLevel, true);
                        container.addBean(newFaLevel);
                        table.refreshRowCache();
                        ElzaNotifications.show("Přidáno...");
                    }),
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
                    new AxAction().caption("Výběr sloupců").icon(FontAwesome.BOOK).run(() ->
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

    private LevelHistoryWindow showVersionHistory(final Integer nodeId) {
        LevelHistoryWindow window = new LevelHistoryWindow(arrangementManager);
        window.show(nodeId, findingAidId);
        return window;
    }

    private DescItemTypeWindow showDescItemTypeWindow() {
        DescItemTypeWindow window = new DescItemTypeWindow(ruleSetManager);
        ArrFaVersion arrFaVersion = arrangementManager.getFaVersionById(version.getId());
        window.show(arrFaVersion);
        return window;
    }
}

