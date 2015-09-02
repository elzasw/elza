package cz.tacr.elza.ui.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.ChildComponentContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;

/**
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class Attribut extends CssLayout implements Components {

    ChildComponentContainer<ArrDescItemExt, AttributDragAndDropWrapper> childs;
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

        Label nadpis = new Label(type.getName());
        nadpis.setSizeUndefined();
        nadpis.addStyleName("label-name");

        addComponent(nadpis);

        childs = new ChildComponentContainer<>();
        childs.addStyleName("container-values");

        addComponent(childs);
        itemExtList.sort((o1, o2) -> o1.getPosition().compareTo(o2.getPosition()));

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
        List<ArrDescItemExt> collect = childs.getChils().stream().map(AttributDragAndDropWrapper::commit).collect(Collectors.toList());
        return collect;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    AttributValue newAtributValue(final ArrDescItemExt a) {
        AttributValue value = new AttributValue(a, descItemSpecs, dataType, AxAction.of(a).icon(FontAwesome.TRASH_O).action(this::deleteAtributValue), AxAction.of(a).icon(FontAwesome.SORT));
        a.setPosition(childs.getComponentCount() + 1);
        a.setNodeId(nodeId);

        AttributDragAndDropWrapper wrapper = new AttributDragAndDropWrapper(value);

        wrapper.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                try {
                    AttributDragAndDropWrapper source = (AttributDragAndDropWrapper) event.getTransferable().getSourceComponent();
                    AttributDragAndDropWrapper target = (AttributDragAndDropWrapper) event.getTargetDetails().getTarget();

                    childs.moveComponent(source, target);
                    precislujPoradi();

                    System.out.println("DragAndDrop " + childs.getKey(source).getPosition() + " >> " + childs.getKey(target).getPosition());
                } catch (Exception e) {
                    System.err.println("Drop error: " + e.getMessage());
                }
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }
        });

        childs.addStyleName("no-horizontal-drag-hints");
        wrapper.setDragStartMode(DragAndDropWrapper.DragStartMode.COMPONENT);

        childs.addComponent(a, wrapper);
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
            AttributDragAndDropWrapper wrapper = (AttributDragAndDropWrapper) childs.getComponent(i);
            childs.getKey(wrapper).setPosition(i + 1);
        }
    }

    public Integer getVersionId() {
        return versionId;
    }
}
