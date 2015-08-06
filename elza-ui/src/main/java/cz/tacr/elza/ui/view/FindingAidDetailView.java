package cz.tacr.elza.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.Item;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;

import cz.req.ax.AxAction;
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleSetManager;
import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.VersionRepository;
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
    private LevelRepository levelRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RuleSetManager ruleSetManager;


    private Integer findingAidId;
    private Integer versionId;
    private FindingAid findingAid;

    AxContainer<ArrangementType> arTypeContainer;
    AxContainer<RuleSet> ruleSetContainer;

    public static final String LEVEL = "Úroveň";
    public static final String LEVEL_POSITION = "Pozice";

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
        }

        this.findingAid = arrangementManager.getFindingAid(findingAidId);

        pageTitle(findingAid.getName());
        addActionsButtons();

        HierarchicalCollapsibleContainer container = new HierarchicalCollapsibleContainer();
        container.addContainerProperty(LEVEL, Integer.class, 0);
        container.addContainerProperty(LEVEL_POSITION, Integer.class, 0);

        final TreeTable table = new TreeTable();
        table.setWidth("100%");

        FaVersion lastVersions = versionRepository.findTopByFindingAid(findingAid);

        List<FaLevel> faLevelsAll = new LinkedList<FaLevel>();

        List<FaLevel> faLevels = levelRepository.findByParentNodeOrderByPositionAsc(lastVersions.getRootNode());
        faLevelsAll.addAll(faLevels);
        //faLevelsAll.addAll(getAllChildByFaLevel(faLevels));

        for (FaLevel faLevel : faLevelsAll) {
            Item item = container.addItem(faLevel.getNodeId());
            item.getItemProperty(LEVEL).setValue(faLevel.getNodeId());
            item.getItemProperty(LEVEL_POSITION).setValue(faLevel.getPosition());
            if (faLevel.getParentNode() != null) {
                container.setParent(faLevel.getNodeId(), faLevel.getParentNode().getNodeId());
            }
            container.setChildrenAllowed(faLevel.getNodeId(), true);
            container.setCollapsed(faLevel.getNodeId(), true);
        }

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

                for (FaLevel faLevel : getChildByFaLevel(levelRepository.findByNodeIdOrderByPositionAsc(itemId))) {
                    Item item = table.addItemAfter(itemIdLast, faLevel.getNodeId());
                    itemIdLast = faLevel.getNodeId();
                    if (faLevel.getParentNode() != null) {
                        container.setParent(faLevel.getNodeId(), faLevel.getParentNode().getNodeId());
                    }
                    item.getItemProperty(LEVEL).setValue(faLevel.getNodeId());
                    item.getItemProperty(LEVEL_POSITION).setValue(faLevel.getPosition());
                    container.setChildrenAllowed(faLevel.getNodeId(), true);
                    container.setCollapsed(faLevel.getNodeId(), true);
                }
            }
        });

        table.setContainerDataSource(container);
        table.setSortEnabled(false);

        components(table);
    }

    private List<FaLevel> getChildByFaLevel(final List<FaLevel> faLevels) {
        List<FaLevel> childs = levelRepository.findByParentNodeInOrderByPositionAsc(faLevels);
        return childs;
    }

    private void removeAllChildren(final TreeTable table, final Integer itemId) {
        Collection<?> children = table.getChildren(itemId);
        if (children != null) {
            ArrayList tmpChildren = new ArrayList(children);
            Iterator<?> itChilder = tmpChildren.iterator();
            while (itChilder.hasNext()) {
                Integer child = (Integer) itChilder.next();
                removeAllChildren(table, child);
                table.removeItem(child);
            }
        }
    }

    private List<FaLevel> getAllChildByFaLevel(final List<FaLevel> faLevels) {
        List<FaLevel> childs = levelRepository.findByParentNodeIn(faLevels);
        if (childs.size() > 0) {
            childs.addAll(getAllChildByFaLevel(childs));
        }
        return childs;
    }

    private void addActionsButtons() {
        actions(
                new AxAction().caption("Přidat záznam").icon(FontAwesome.PLUS).run(() -> {
                    //TODO Implementace
                    throw new UnsupportedOperationException();
                }),
                new AxAction().caption("Zobrazit historii").icon(FontAwesome.HISTORY).run(() ->
                navigate(VersionListView.class, getParameterInteger())),
                new AxAction().caption("Schválit verzi").icon(FontAwesome.HISTORY).run(() -> {
                    AxForm<VOApproveVersion> formularApproveVersion = formularApproveVersion();
                    FaVersion version = new FaVersion();
                    VOApproveVersion appVersion = new VOApproveVersion();
                    appVersion.setArrangementTypeId(version.getArrangementType().getArrangementTypeId());
                    appVersion.setRuleSetId(version.getRuleSet().getRuleSetId());

                    approveVersion(formularApproveVersion, appVersion);
                })
                );
    }

    private void approveVersion(final AxForm<VOApproveVersion> form, final VOApproveVersion appVersion) {
        form.setValue(appVersion);
        new AxWindow().components(form)
        .buttonPrimary(new AxAction<VOApproveVersion>()
                .caption("Uložit")
                .exception(ex -> {
                    ex.printStackTrace();
                })
                .primary()
                .value(form::commit)
                .action(this::approveVersion)
                ).buttonClose().modal().style("fa-window-detail").show();

    }

    private void approveVersion(final VOApproveVersion voApproveVersion) {
        versionId = arrangementManager.approveVersion(findingAidId, voApproveVersion.getArrangementTypeId(),
                voApproveVersion.getRuleSetId()).getFaVersionId();
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularApproveVersion")
    AxForm<VOApproveVersion> formularApproveVersion() {
        AxForm<VOApproveVersion> form = AxForm.init(VOApproveVersion.class);
        form.addStyleName("fa-form");
        form.setCaption("Schválení verze archivní pomůcky");

        arTypeContainer = new AxContainer<>(ArrangementType.class).supplier(arrangementManager::getArrangementTypes);
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, ArrangementType::getName).required();

        ruleSetContainer = new AxContainer<>(RuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RuleSet::getName).required();
        return form;
    }
}

