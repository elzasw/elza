package cz.tacr.elza.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Item;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeTable;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.VersionRepository;
import cz.tacr.elza.ui.ElzaView;
import ru.xpoft.vaadin.VaadinView;


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

    private Integer findingAidId;
    private FindingAid findingAid;

    public static final String LEVEL = "Úroveň";
    public static final String LEVEL_POSITION = "Pozice";

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        findingAidId = getParameterInteger();
        if (findingAidId == null) {
            navigate(FindingAidListView.class);
            return;
        }

        this.findingAid = arrangementManager.getFindingAid(findingAidId);

        addBodyHead();
        addActionsButtons();

        HierarchicalCollapsibleContainer container = new HierarchicalCollapsibleContainer();
        container.addContainerProperty(LEVEL, Integer.class, 0);
        container.addContainerProperty(LEVEL_POSITION, Integer.class, 0);

        final TreeTable table = new TreeTable();
        table.setWidth("100%");

        FaVersion lastVersions = versionRepository.findTopByFindingAidId(findingAidId);

        List<FaLevel> faLevelsAll = new LinkedList<FaLevel>();

        List<FaLevel> faLevels = levelRepository.findByParentNodeIdOrderByPositionAsc(lastVersions.getRootNodeId());
        faLevelsAll.addAll(faLevels);
        //faLevelsAll.addAll(getAllChildByFaLevel(faLevels));

        for (FaLevel faLevel : faLevelsAll) {
            Item item = container.addItem(faLevel.getNodeId());
            item.getItemProperty(LEVEL).setValue(faLevel.getNodeId());
            item.getItemProperty(LEVEL_POSITION).setValue(faLevel.getPosition());
            if (faLevel.getParentNode() != null) {
                container.setParent(faLevel.getNodeId(), faLevel.getParentNodeId());
            }
            container.setChildrenAllowed(faLevel.getNodeId(), true);
            container.setCollapsed(faLevel.getNodeId(), true);
        }

        table.addCollapseListener(new Tree.CollapseListener() {
            @Override
            public void nodeCollapse(Tree.CollapseEvent collapseEvent) {
                Integer itemId = (Integer) collapseEvent.getItemId();
                removeAllChildren(table, itemId);
            }
        });

        table.addExpandListener(new Tree.ExpandListener() {

            public void nodeExpand(Tree.ExpandEvent expandEvent) {
                Integer itemId = (Integer) expandEvent.getItemId();

                Integer itemIdLast = itemId;

                for (FaLevel faLevel : getChildByFaLevel(levelRepository.findByNodeIdOrderByPositionAsc(itemId))) {
                    Item item = table.addItemAfter(itemIdLast, faLevel.getNodeId());
                    itemIdLast = faLevel.getNodeId();
                    if (faLevel.getParentNode() != null) {
                        container.setParent(faLevel.getNodeId(), faLevel.getParentNodeId());
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

        bodyMain().addComponent(table);
    }

    private List<FaLevel> getChildByFaLevel(List<FaLevel> faLevels) {
        List<Integer> faLevelsNodeIds = new LinkedList<>();
        for (FaLevel faLevel : faLevels) {
            faLevelsNodeIds.add(faLevel.getNodeId());
        }

        List<FaLevel> childs = levelRepository.findByParentNodeIdInOrderByPositionAsc(faLevelsNodeIds);
        return childs;
    }

    private void removeAllChildren(TreeTable table, Integer itemId) {
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

    private List<FaLevel> getAllChildByFaLevel(List<FaLevel> faLevels) {
        List<Integer> faLevelsNodeIds = new LinkedList<>();
        for (FaLevel faLevel : faLevels) {
            faLevelsNodeIds.add(faLevel.getNodeId());
        }

        List<FaLevel> childs = levelRepository.findByParentNodeIdIn(faLevelsNodeIds);
        if (childs.size() > 0) {
            childs.addAll(getAllChildByFaLevel(childs));
        }
        return childs;
    }

    private void addActionsButtons() {
        Button addRecord = new Button("Přidat záznam");
        addRecord.addStyleName("button");
        addRecord.addStyleName("button-add");
        Button showHistory = new Button("Zobrazit historii");
        showHistory.addStyleName("button");
        showHistory.addStyleName("button-show");
        showHistory.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                navigate(VersionListView.class, getParameterInteger());
            }
        });
        Button approveVersion = new Button("Schválit verzi");
//        approveVersion.addStyleName("button");
        approveVersion.addStyleName("button-approve");


        actionsBar().addComponent(addRecord);
        actionsBar().addComponent(showHistory);
        actionsBar().addComponent(approveVersion);
    }

    public void addBodyHead() {
        Label title = new Label("<h1>" + findingAid.getName() + "</h1>");
        title.setContentMode(ContentMode.HTML);
        CssLayout titleBar = new CssLayout(title);
        titleBar.addStyleName("title");
        bodyHeadMain().addComponent(titleBar);
    }
}

