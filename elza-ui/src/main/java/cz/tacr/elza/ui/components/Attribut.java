package cz.tacr.elza.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.ChildComponentContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;


public class Attribut extends CssLayout implements Components {

    ChildComponentContainer<ArrDescItemExt, AttributValue> childs;
    List<RulDescItemSpec> descItemSpecs;
    RulDataType dataType;
    Integer nodeId;

    List<ArrDescItemExt> deleteDescItems;
    private Integer versionId;

    public Attribut(List<ArrDescItemExt> itemExtList, List<RulDescItemSpec> descItemSpecs, RulDescItemType type, RulDataType dataType, Integer nodeId, Integer versionId) {
        this.descItemSpecs = descItemSpecs;
        this.dataType = dataType;
        this.nodeId = nodeId;
        this.versionId = versionId;
        deleteDescItems = new ArrayList<>();

        addComponent(new Label(type.getName()));

        childs = new ChildComponentContainer<>();
        childs.addStyleName("container-values");

        addComponent(childs);
        for (ArrDescItemExt descItemExt : itemExtList) {
            newAtributValue(descItemExt);
        }

        AxAction newValueButton = new AxAction().run(() -> {
            ArrDescItemExt descItem = new ArrDescItemExt();
            descItem.setDescItemType(type);
            newAtributValue(descItem);
        }).caption("Přidat další hodnotu").style("new-attribut-button").icon(FontAwesome.PLUS);

        addComponent(newValueButton.button());
    }


    public List<ArrDescItemExt> getKeys() {
        List<ArrDescItemExt> collect = childs.getChils().stream().map(AttributValue::commit).collect(Collectors.toList());
        return collect;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    AttributValue newAtributValue(final ArrDescItemExt a) {
        AttributValue value = new AttributValue(a, descItemSpecs, dataType, AxAction.of(a).icon(FontAwesome.TRASH_O).action(this::deleteAtributValue));
        a.setPosition(childs.getComponentCount() + 1);
        a.setNodeId(nodeId);
        childs.addComponent(a, value);
        return value;
    }

    private void deleteAtributValue(final ArrDescItemExt a) {
        childs.removeComponent(a);
        if (a.getDescItemObjectId() != null) {
            deleteDescItems.add(a);
        }
        precislujPoradi();
    }

    public List<ArrDescItemExt> getDeleteDescItems() {
        return deleteDescItems;
    }

    public void precislujPoradi() {
        for (int i = 0; i < childs.getComponentCount(); i++) {
            AttributValue attributValue = (AttributValue) childs.getComponent(i);
            childs.getKey(attributValue).setPosition(i + 1);
        }
    }

    public Integer getVersionId() {
        return versionId;
    }
}
