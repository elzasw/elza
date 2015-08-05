package cz.tacr.elza.ui.view;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Item;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
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

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        findingAidId = getParameterInteger();
        if (findingAidId == null) {
            navigate(FindingAidListView.class);
            return;
        }

        for(FindingAid findingAid : arrangementManager.getFindingAids()) {
            if(findingAid.getFindigAidId().equals(findingAidId)) {
                this.findingAid = findingAid;
            }
        }

//        bodyHead().addComponent(test);

        addBodyHead();
        addActionsButtons();

        //TreeTable table = new TreeTable();

        final TreeTable table = new TreeTable();

        HierarchicalCollapsibleContainer container = new HierarchicalCollapsibleContainer();
        container.addContainerProperty("CAPTION", String.class, "--");
        table.setContainerDataSource(container);

        /*Item a = container.addItem("a");
        a.getItemProperty("CAPTION").setValue("a");
        Item b = container.addItem("b");
        b.getItemProperty("CAPTION").setValue("b");
        Item c = container.addItem("c");
        c.getItemProperty("CAPTION").setValue("c");

        container.setChildrenAllowed(a, true);
        container.setChildrenAllowed(b, true);
        container.setChildrenAllowed(c, false);


        container.setParent("b", "a");
        container.setParent("c", "b");*/

        List<FaVersion> versions = versionRepository.findByFindingAidId(findingAidId);

        List<FaLevel> faLevelsAll = new LinkedList<FaLevel>();

        for(FaVersion faVersion : versions) {
            List<FaLevel> faLevels = levelRepository.findByFaLevelId(faVersion.getRootNodeId());
            faLevelsAll.addAll(faLevels);
        }

        //List<FaLevel> levels = levelRepository.findByFaLevelIds();

        //container.add(levels);

        bodyMain().addComponent(table);

    }

    private void addActionsButtons() {
        Button addRecord = new Button("Přidat záznam");
        addRecord.addStyleName("fa-button");
        addRecord.addStyleName("fa-button-add");
        Button showHistory = new Button("Zobrazit historii");
        showHistory.addStyleName("fa-button");
        showHistory.addStyleName("fa-button-show");
        Button approveVersion = new Button("Schválit verzi");
        approveVersion.addStyleName("fa-button");
        approveVersion.addStyleName("fa-button-approve");


        actionsBar().addComponent(addRecord);
        actionsBar().addComponent(showHistory);
        actionsBar().addComponent(approveVersion);
    }

    public void addBodyHead() {
        Label title = new Label("<h1>" + findingAid.getName() + "</h1>");
        title.setContentMode(ContentMode.HTML);
        CssLayout titleBar = new CssLayout(title);
        titleBar.addStyleName("fa-title");
        bodyHeadMain().addComponent(titleBar);
    }
}

