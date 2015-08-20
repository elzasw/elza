package cz.tacr.elza.ui.view;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.ui.utils.ElzaNotifications;
import cz.tacr.elza.ui.window.LevelHistoryWindow;
import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.Item;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
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
import cz.tacr.elza.controller.RuleSetManager;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.ui.ElzaView;


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
    private RuleSetManager ruleSetManager;

    private Integer findingAidId;
    private Integer versionId;

    private ArrFindingAid findingAid;
    private Integer levelNodeIdVyjmout;

    AxContainer<ArrArrangementType> arTypeContainer;
    AxContainer<RulRuleSet> ruleSetContainer;

    private TreeTable table;

    public static final String LEVEL = "Úroveň";
    public static final String LEVEL_POSITION = "Pozice";
    public static final String ACTION = "Akce";

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        Integer[] params = getParameterIntegers();
        if (params.length == 0) {
            navigate(FindingAidListView.class);
            return;
        }
        findingAidId = params[0];

        if (params.length > 1) {
            versionId = params[1];
        } else {
            versionId = null;
        }

        this.findingAid = arrangementManager.getFindingAid(findingAidId);
        discardNodeCut();


        pageTitle(findingAid.getName());
        addActionsButtons(versionId != null);

        HierarchicalCollapsibleContainer container = new HierarchicalCollapsibleContainer();
        container.addContainerProperty(LEVEL, Integer.class, 0);
        container.addContainerProperty(LEVEL_POSITION, Integer.class, 0);

        table = new TreeTable();
        table.addStyleName("detail-table");
        table.setWidth("100%");

        table.setContainerDataSource(container);
        table.setSortEnabled(false);
        table.setPageLength(20);
        table.addGeneratedColumn(LEVEL, new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                return "Jednotka archivního popisu číslo " + itemId;
            }
        });

        ArrFaVersion version = getVersion();
        if (versionId == null) {
            addActionMenu(container);
            table.setVisibleColumns(LEVEL, LEVEL_POSITION, ACTION);
        } else {
            addActionHistMenu(container);
            table.setVisibleColumns(LEVEL, LEVEL_POSITION, ACTION);
            if (version.getFindingAid() == null
                    || (!version.getFindingAid().getFindingAidId().equals(findingAidId))) {
                version = null;
            }
        }

        if (version == null) { // chyba nenalezena verze k FA
            navigate(FindingAidListView.class);
            return;
        }

        refreshTree(container, version.getRootNode());

        table.addCollapseListener(new Tree.CollapseListener() {
            @Override
            public void nodeCollapse(final Tree.CollapseEvent collapseEvent) {
                Integer itemId = (Integer) collapseEvent.getItemId();
                removeAllChildren(table, itemId);
            }
        });

        table.addExpandListener(new Tree.ExpandListener() {

            @Override
            public void nodeExpand(final Tree.ExpandEvent expandEvent) {
                Integer itemId = (Integer) expandEvent.getItemId();

                Integer itemIdLast = itemId;

                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(itemId, versionId);
                for (ArrFaLevel faLevel : faLevels) {
                    if (container.containsId(faLevel.getNodeId())) {
                        break;
                    }
                    Item item = table.addItemAfter(itemIdLast, faLevel.getNodeId());
                    itemIdLast = faLevel.getNodeId();
                    initNewItemInContainer(item, faLevel, container);
                }
            }
        });



        if (version != null && version.getLockChange() != null && version.getLockChange().getChangeDate() != null) {
            String createDataStr = version.getLockChange().getChangeDate()
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            com.vaadin.ui.Component verzeComponent = newLabel("Prohlížíte si uzavřenou verzi k " + createDataStr, "h2");
            components(verzeComponent, table);
        } else {
            components(table);
        }
    }

    private void addActionMenu(HierarchicalCollapsibleContainer container) {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                MenuBar.MenuItem child = new AxAction().caption("Přidat záznam před").icon(FontAwesome.PLUS).run(() -> {
                            discardNodeCut();

                            ArrFaLevel level = arrangementManager.addLevelBefore((Integer) itemId);
                            Item item = container
                                    .addItemAt(container.indexOfId(itemId), level.getNodeId());
                            initNewItemInContainer(item, level, container);
                            repositionLowerSiblings(level.getNodeId(),
                                    (Integer) item.getItemProperty(LEVEL_POSITION).getValue() + 1,
                                    container);
                            ElzaNotifications.show("Přidáno...");
                        }
                ).menuItem(parent);



                child = new AxAction().caption("Přidat záznam za").icon(FontAwesome.PLUS).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        Integer lastId = getItemIdAfterChilds((Integer) itemId, container);

                        ArrFaLevel newFaLevel = arrangementManager.addLevelAfter((Integer) itemId);

                        Item item = container.getItem(itemId);
                        repositionLowerSiblings((Integer) itemId,
                                (Integer) item.getItemProperty(LEVEL_POSITION).getValue() + 2,
                                container);

                        addItemAfterToContainer(newFaLevel, container, lastId);


                        ElzaNotifications.show("Přidáno...");
                    }
                }).menuItem(parent);

                child = new AxAction().caption("Přidat podřízený záznam").icon(FontAwesome.PLUS).run(() -> {
                    discardNodeCut();
                    if (table.isCollapsed(itemId)) {
                        table.setCollapsed(itemId, false);
                    }

                    ArrFaLevel newFaLevel = arrangementManager.addLevelChild((Integer) itemId);

                    Object itemIdLast = itemId;
                    Collection<?> children = container.getChildren(itemId);
                    if (!CollectionUtils.isEmpty(children)) {
                        Iterator<?> iterator = children.iterator();
                        while (iterator.hasNext()) {
                            itemIdLast = iterator.next();
                        }
                    }

                    Item item = table.addItemAfter(itemIdLast, newFaLevel.getNodeId());
                    itemIdLast = newFaLevel.getNodeId();
                    initNewItemInContainer(item, newFaLevel, container);
                    ElzaNotifications.show("Přidáno...");
                }).menuItem(parent);

                child = new AxAction().caption("Smazat").icon(FontAwesome.TRASH_O).run(new Runnable() {
                    @Override
                    public void run() {
                        discardNodeCut();
                        arrangementManager.deleteLevel((Integer) itemId);

                        Integer position = (Integer) container.getItem(itemId)
                                .getItemProperty(LEVEL_POSITION).getValue();
                        repositionLowerSiblings((Integer) itemId, position, container);

                        table.removeItem(itemId);

                        ElzaNotifications.show("Smazáno...");
                    }
                }).menuItem(parent);

                child = new AxAction().caption("Vyjmout").icon(FontAwesome.CUT).run(() -> {
                    cutNode((Integer) itemId);
                    ElzaNotifications.show("Ve schránce...");
                }).menuItem(parent);

                child = new AxAction().caption("Vložit před").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {
                            ArrFaLevel level = arrangementManager
                                    .moveLevelBefore(levelNodeIdVyjmout, (Integer) itemId);
                            Integer position = (Integer) container.getItem(levelNodeIdVyjmout)
                                    .getItemProperty(LEVEL_POSITION).getValue();
                            repositionLowerSiblings(levelNodeIdVyjmout, position, container);
                            table.removeItem(levelNodeIdVyjmout);

                            addItemBeforeToContainer(level, container, itemId);
                            repositionLowerSiblings(level.getNodeId(), level.getPosition() + 1, container);
                            discardNodeCut();

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).menuItem(parent);
                child.setStyleName("show-if-cut");

                child = new AxAction().caption("Vložit za").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {
                            arrangementManager.moveLevelAfter(levelNodeIdVyjmout, (Integer) itemId);

                            Integer position = (Integer) container.getItem(levelNodeIdVyjmout)
                                    .getItemProperty(LEVEL_POSITION).getValue();
                            repositionLowerSiblings(levelNodeIdVyjmout, position, container);
                            table.removeItem(levelNodeIdVyjmout);


                            ArrFaLevel faLevelVyjmout = arrangementManager.findLevelByNodeId(
                                    levelNodeIdVyjmout);

                            Item item = container.getItem(itemId);
                            repositionLowerSiblings((Integer) itemId,
                                    (Integer) item.getItemProperty(LEVEL_POSITION).getValue() + 2,
                                    container);

                            addItemAfterToContainer(faLevelVyjmout, container,
                                    getItemIdAfterChilds((Integer) itemId, container));


                            discardNodeCut();

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).menuItem(parent);
                child.setStyleName("show-if-cut");


                child = new AxAction().caption("Vložit jako podřízený").icon(FontAwesome.PASTE).run(() -> {
                    try {
                        if (checkPaste()) {
                            arrangementManager.moveLevelUnder(levelNodeIdVyjmout, (Integer) itemId);

                            Integer position = (Integer) container.getItem(levelNodeIdVyjmout)
                                    .getItemProperty(LEVEL_POSITION).getValue();
                            repositionLowerSiblings((Integer) levelNodeIdVyjmout, position, container);
                            table.removeItem(levelNodeIdVyjmout);

                            ArrFaLevel faLevelVyjmout = arrangementManager.findLevelByNodeId(
                                    levelNodeIdVyjmout);

                            if (container.isCollapsed(itemId)) {
                                List<ArrFaLevel> faLevels = arrangementManager.findSubLevels((Integer) itemId, versionId);
                                Integer idLast = (Integer) itemId;
                                for (ArrFaLevel faLevel : faLevels) {
                                    idLast = addItemAfterToContainer(faLevel, container, idLast);
                                }
                            } else {

                                //najdeme posledního přímého potomka
                                Object itemIdLast = itemId;
                                Collection<?> children = container.getChildren(itemId);
                                if (!CollectionUtils.isEmpty(children)) {
                                    Iterator<?> iterator = children.iterator();
                                    while (iterator.hasNext()) {
                                        itemIdLast = iterator.next();
                                    }
                                }
                                addItemAfterToContainer(faLevelVyjmout, container, itemIdLast);
                            }

                            discardNodeCut();

                            ElzaNotifications.show("Přesunuto...");
                        }
                    } catch (IllegalStateException e) {
                        ElzaNotifications.showWarn(e.getMessage());
                    } catch (IllegalArgumentException e) {
                        ElzaNotifications.showError(e.getMessage());
                    }
                }).menuItem(parent);
                child.setStyleName("show-if-cut");

                child = new AxAction().caption("Zobrazit historii").icon(FontAwesome.CALENDAR).run(() -> {
                    showVersionHistory((Integer) itemId);
                }).menuItem(parent);
                return menuBar;
            }
        });
    }

    private void addActionHistMenu(HierarchicalCollapsibleContainer container) {
        table.addGeneratedColumn("Akce", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // TODO: změnit celou tabulku na Ax

                MenuBar menuBar = new MenuBar();

                MenuBar.MenuItem parent = menuBar.addItem("", FontAwesome.ALIGN_JUSTIFY, null);
                MenuBar.MenuItem child = new AxAction().caption("Zobrazit historii").icon(FontAwesome.CALENDAR).run(() -> {
                    showVersionHistory((Integer) itemId);
                }).menuItem(parent);
                return menuBar;
            }
        });
    }

    private boolean checkPaste(){
        if(levelNodeIdVyjmout == null){
            ElzaNotifications.show("Není co vložit, nejprve je potřeba vyjmout uzel.");
            return false;
        }else{
            return true;
        }
    }

    private void refreshTree(final HierarchicalCollapsibleContainer container, final ArrFaLevel rootLevel) {
        container.removeAllItems();
        List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(rootLevel.getNodeId(), versionId);

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
    private Integer getItemIdAfterChilds(final Integer itemId, final HierarchicalCollapsibleContainer container) {

        Integer lastId = itemId;

        Collection<?> childs = container.getChildren(lastId);
        if (!CollectionUtils.isEmpty(childs)) {
            Iterator<?> iterator = childs.iterator();
            Object child = iterator.next();
            while (iterator.hasNext()) {
                child = iterator.next();
            }
            lastId = getItemIdAfterChilds((Integer) child, container);
        }

        return lastId;
    }


    private Integer addItemAfterToContainer(final ArrFaLevel level, final HierarchicalCollapsibleContainer container, final Object itemIdBefore){
        Item item = container.addItemAfter(itemIdBefore, level.getNodeId());
        initNewItemInContainer(item, level, container);

        Integer lastId = level.getNodeId();
        if (!container.isCollapsed(level.getNodeId())) {
            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), versionId);

            for (ArrFaLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }
        }
        return lastId;
    }

    private Integer addItemBeforeToContainer(final ArrFaLevel level, final HierarchicalCollapsibleContainer container, final Object itemIdAfter){
        Item item = container.addItemAt(container.indexOfId(itemIdAfter), level.getNodeId());
        initNewItemInContainer(item, level, container);

        Integer lastId = level.getNodeId();
        if (!container.isCollapsed(level.getNodeId())) {
            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), versionId);

            for (ArrFaLevel faLevel : faLevels) {
                lastId = addItemAfterToContainer(faLevel, container, lastId);
            }
        }
        return lastId;
    }


    private void addItemToContainer(final ArrFaLevel level, final HierarchicalCollapsibleContainer container) {
        Item item = container.addItem(level.getNodeId());
        initNewItemInContainer(item, level, container);

        if (!container.isCollapsed(level.getNodeId())) {
            List<ArrFaLevel> faLevels = arrangementManager.findSubLevels(level.getNodeId(), versionId);

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
    private void repositionLowerSiblings(final Integer itemId,
            final int position,
            final HierarchicalCollapsibleContainer container) {
        Collection<Integer> lowerSiblings = container.getLowerSiblings(itemId);

        int index = position;
        for (Integer lowerSibling : lowerSiblings) {
            container.getItem(lowerSibling).getItemProperty(LEVEL_POSITION)
            .setValue(index++);
        }
    }


    /**
     * Nastaví novou položku v konejneru.
     *
     * @param item      nová položka
     * @param faLevel   level
     * @param container kontejner
     */
    private void initNewItemInContainer(final Item item,
            final ArrFaLevel faLevel,
            final HierarchicalCollapsibleContainer container) {
        item.getItemProperty(LEVEL).setValue(faLevel.getNodeId());
        item.getItemProperty(LEVEL_POSITION).setValue(faLevel.getPosition());
        if(faLevel.getParentNodeId().equals(getVersion().getRootNode().getNodeId())){
            //hack kvůli chybě ve vaadin, aby byl vložen prvek do seznamu rootů
            if (faLevel.getNodeId().equals(container.firstItemId())) {
                container.setParent(faLevel.getNodeId(), container.lastItemId());
            } else {
                container.setParent(faLevel.getNodeId(), container.firstItemId());
            }

            container.setParent(faLevel.getNodeId(), null);
        } else if (faLevel.getParentNodeId() != null) {
            container.setParent(faLevel.getNodeId(), faLevel.getParentNodeId());
        }
        container.setChildrenAllowed(faLevel.getNodeId(), true);
        container.setCollapsed(faLevel.getNodeId(), container.isCollapsed(faLevel.getNodeId()));
    }

    private ArrFaVersion getVersion(){
        ArrFaVersion version;
        if (versionId == null) {
            version = arrangementManager.getOpenVersionByFindingAidId(findingAidId);
        } else {
            version = arrangementManager.getFaVersionById(versionId);
        }
        return version;
    }

    private void removeAllChildren(final TreeTable table, final Integer itemId) {
        Collection<?> children = table.getChildren(itemId);
        if (children != null) {
            ArrayList tmpChildren = new ArrayList(children);
            Iterator<?> itChilder = tmpChildren.iterator();
            while (itChilder.hasNext()) {
                Integer child = (Integer) itChilder.next();
                table.removeItem(child);
            }
        }
    }

    private void addActionsButtons(boolean historiOnly) {
        if (historiOnly) {
            AxAction hist = new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
            navigate(VersionListView.class, findingAidId));
            actions(hist);
        } else {
            actions(
                    new AxAction().caption("Přidat záznam").icon(FontAwesome.PLUS).run(() -> {

                        ArrFaLevel newFaLevel = arrangementManager.addLevel(findingAidId);

                        Item item = table.addItem(newFaLevel.getNodeId());

                        HierarchicalCollapsibleContainer container = (HierarchicalCollapsibleContainer) table.getContainerDataSource();

                        if (newFaLevel.getParentNodeId() != null) {
                            container.setParent(newFaLevel.getNodeId(), newFaLevel.getParentNodeId());
                        }
                        item.getItemProperty(LEVEL).setValue(newFaLevel.getNodeId());
                        item.getItemProperty(LEVEL_POSITION).setValue(newFaLevel.getPosition());
                        container.setChildrenAllowed(newFaLevel.getNodeId(), true);
                        container.setCollapsed(newFaLevel.getNodeId(), true);

                        ElzaNotifications.show("Přidáno...");

                    }),
                    new AxAction().caption("Zobrazit verze").icon(FontAwesome.HISTORY).run(() ->
                    navigate(VersionListView.class, findingAidId)),
                    new AxAction().caption("Uzavřít verzi").icon(FontAwesome.HISTORY).run(() -> {
                        AxForm<VOApproveVersion> formularApproveVersion = formularApproveVersion();
                        ArrFaVersion version = arrangementManager.getOpenVersionByFindingAidId(findingAid.getFindingAidId());
                        VOApproveVersion appVersion = new VOApproveVersion();
                        appVersion.setArrangementTypeId(version.getArrangementType().getArrangementTypeId());
                        appVersion.setRuleSetId(version.getRuleSet().getRuleSetId());

                        approveVersion(formularApproveVersion, appVersion);
                    })
                    );
        }
    }

    private void approveVersion(final AxForm<VOApproveVersion> form, final VOApproveVersion appVersion) {
        form.setValue(appVersion);
        new AxWindow().caption("Uzavření verze archivní pomůcky").components(form)
        .buttonClose().buttonPrimary(new AxAction<VOApproveVersion>()
                .caption("Uložit")
                .exception(ex -> {
                    ex.printStackTrace();
                })
                .primary()
                .value(form::commit)
                .action(this::approveVersion)
                ).modal().style("fa-window-detail").show();

    }

    private void approveVersion(final VOApproveVersion voApproveVersion) {
        versionId = arrangementManager.approveVersion(findingAidId, voApproveVersion.getArrangementTypeId(),
                voApproveVersion.getRuleSetId()).getFaVersionId();
    }

    private void cutNode(final Integer itemId){
        UI.getCurrent().setOverlayContainerLabel("");
        levelNodeIdVyjmout = itemId;
    }

    private void discardNodeCut(){
        UI.getCurrent().setOverlayContainerLabel("item-not-cut");
        levelNodeIdVyjmout = null;
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularApproveVersion")
    private AxForm<VOApproveVersion> formularApproveVersion() {
        AxForm<VOApproveVersion> form = AxForm.init(VOApproveVersion.class);
        form.addStyleName("fa-form");

        arTypeContainer = new AxContainer<>(ArrArrangementType.class).supplier(arrangementManager::getArrangementTypes);
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
}

