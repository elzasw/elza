package cz.tacr.elza.ui.components.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.ChildComponentContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class Attribut extends CssLayout implements Components {

    ChildComponentContainer<ArrDescItemExt, AttributValue> childs;
    List<RulDescItemSpec> descItemSpecs;
    RulDataType dataType;
    ArrNode node;

    List<ArrDescItemExt> deleteDescItems;
    private Integer versionId;
    private RulDescItemType type;
    private AxAction newValueButton;
    private AttributeValuesLoader attributeValuesLoader;

    public Attribut(List<ArrDescItemExt> itemExtList,
                    List<RulDescItemSpec> descItemSpecs,
                    RulDescItemType type,
                    RulDataType dataType,
                    ArrNode node,
                    Integer versionId,
                    final AttributeValuesLoader attributeValuesLoader) {
        this.descItemSpecs = descItemSpecs;
        this.dataType = dataType;
        this.node = node;
        this.versionId = versionId;
        this.type = type;
        this.attributeValuesLoader = attributeValuesLoader;
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

        newValueButton = new AxAction().run(() -> {
            ArrDescItemExt descItem = new ArrDescItemExt();
            descItem.setDescItemType(type);
            newAtributValue(descItem);
        }).caption("Přidat další hodnotu").style("new-attribut-button").icon(FontAwesome.PLUS);

        addComponent(newValueButton.button());

        // přidá nový řádek, pokud nebyl žádný (ulehčení pro uživatele)
        if (itemExtList.size() == 0) {
            ArrDescItemExt descItem = new ArrDescItemExt();
            descItem.setDescItemType(type);
            newAtributValue(descItem);
        }
    }


    public List<ArrDescItemExt> getKeys() {
        List<ArrDescItemExt> collect = childs.getChils().stream().map(AttributValue::commit).collect(Collectors.toList());
        for (ArrDescItemExt descItemExt : collect) {
            if (descItemExt.getData() == null) {
                descItemExt.setData(new String());
            }
        }
        return collect;
    }

    public ArrNode getNode() {
        return node;
    }

    AttributValue newAtributValue(final ArrDescItemExt a) {

        Label sortIcon = new Label();
        sortIcon.addStyleName("sort-icon");
        sortIcon.setContentMode(ContentMode.HTML);
        sortIcon.setValue(FontAwesome.SORT.getHtml());
        sortIcon.setSizeUndefined();

        DragAndDropWrapper wrapper = new DragAndDropWrapper(sortIcon);

        wrapper.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                try {
                    DragAndDropWrapper sourceWrapper = (DragAndDropWrapper) event.getTransferable().getSourceComponent();
                    DragAndDropWrapper targetWrapper = (DragAndDropWrapper) event.getTargetDetails().getTarget();

                    AttributValue source = null;
                    AttributValue target = null;

                    for (AttributValue attributValue : childs.getChils()) {
                        if (attributValue.getWrapper().equals(sourceWrapper)) {
                            source = attributValue;
                        }
                        if (attributValue.getWrapper().equals(targetWrapper)) {
                            target = attributValue;
                        }
                    }

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
        wrapper.setSizeUndefined();

        AttributValue value = new AttributValue(a, descItemSpecs, dataType, AxAction.of(a).icon(FontAwesome.TRASH_O).action(this::deleteAtributValue), wrapper, attributeValuesLoader);
        a.setPosition(childs.getComponentCount() + 1);
        a.setNode(node);
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
            AttributValue av = (AttributValue) childs.getComponent(i);
            childs.getKey(av).setPosition(i + 1);
        }
    }

    public Integer getVersionId() {
        return versionId;
    }
}
