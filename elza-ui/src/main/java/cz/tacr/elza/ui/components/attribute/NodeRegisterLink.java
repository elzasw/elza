package cz.tacr.elza.ui.components.attribute;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import cz.req.ax.AxAction;
import cz.req.ax.ChildComponentContainer;
import cz.req.ax.Components;
import cz.tacr.elza.controller.RegistryManager;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RegRegisterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 *
 */
public class NodeRegisterLink extends CssLayout implements Components {

    ChildComponentContainer<ArrNodeRegister, NodeRegisterLinkValue> childs;
    ArrNode node;
    private List<ArrNodeRegister> toDelete = new ArrayList<>();
    private Integer versionId;
    private RegistryManager registryManager;
    private AxAction newValueButton;

    public NodeRegisterLink(final ArrNode node, final Integer versionId, final List<ArrNodeRegister> vazby,
                            final RegistryManager registryManager) {

        this.node = node;
        this.versionId = versionId;
        this.registryManager = registryManager;

        addStyleName("table-hierarchy");

        Label nadpis = new Label("Vazba na hesla rejstříku");
        nadpis.setSizeUndefined();
        nadpis.addStyleName("label-name");

        addComponent(nadpis);

        childs = new ChildComponentContainer<>();
        childs.addStyleName("container-values");

        addComponent(childs);
        vazby.sort(new NodeRegisterLinkValuesComparator());

        for (final ArrNodeRegister nodeRegister : vazby) {
            newRegisterLinkValue(nodeRegister);
        }

        newValueButton = new AxAction().run(() -> {
            ArrNodeRegister nodeRegister = new ArrNodeRegister();
            nodeRegister.setNode(node);
            newRegisterLinkValue(nodeRegister);
        }).caption("Přidat další hodnotu").icon(FontAwesome.PLUS);

        addComponent(cssLayout("", newValueButton.button()));

        // přidá nový řádek, pokud nebyl žádný (ulehčení pro uživatele)
        if (vazby.size() == 0) {
            ArrNodeRegister nodeRegister = new ArrNodeRegister();
            nodeRegister.setNode(node);
            newRegisterLinkValue(nodeRegister);
        }
    }

    public List<ArrNodeRegister> getKeys() {
        List<ArrNodeRegister> collect = childs.getChils().stream().map(NodeRegisterLinkValue::commit).collect(Collectors.toList());
        return collect;
    }

    public List<ArrNodeRegister> getLinksToDelete() {
        return toDelete;
    }

    public ArrNode getNode() {
        return node;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public NodeRegisterLinkValue newRegisterLinkValue(final ArrNodeRegister nodeRegister) {

        Label sortIcon = new Label();
        sortIcon.addStyleName("sort-icon");
        sortIcon.setContentMode(ContentMode.HTML);
        sortIcon.setValue(FontAwesome.SORT.getHtml());
        sortIcon.setSizeUndefined();

        List<RegRegisterType> registerTypes = registryManager.getRegisterTypes();
        NodeRegisterLinkValue value = new NodeRegisterLinkValue(nodeRegister, registerTypes,
                AxAction.of(nodeRegister).icon(FontAwesome.TRASH_O).action(this::deleteAtributValue), registryManager);
        nodeRegister.setNode(node);
        childs.addComponent(nodeRegister, value);
        return value;
    }

    private void deleteAtributValue(final ArrNodeRegister nodeRegister) {
        childs.removeComponent(nodeRegister);
        if (nodeRegister.getNodeRegisterId() != null) {
            toDelete.add(nodeRegister);
        }
    }

}
