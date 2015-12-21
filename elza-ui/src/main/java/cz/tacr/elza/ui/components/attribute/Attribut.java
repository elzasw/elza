package cz.tacr.elza.ui.components.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.util.Assert;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;

import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.AxWindow;
import cz.req.ax.ChildComponentContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemDecimal;
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPacketRef;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * Komponenta pro atribut.
 *
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public abstract class Attribut extends CssLayout implements Components {

    /**
     * Kontejner pro atribut.
     */
    ChildComponentContainer<ArrDescItem, AttributValue> childs;

    /**
     * Seznam specifikací.
     */
    List<RulDescItemSpec> descItemSpecs;

    /**
     * Typ dat.
     */
    RulDataType dataType;

    /**
     * Uzel.
     */
    ArrNode node;

    /**
     * Seznam smazaných atributů.
     */
    List<ArrDescItem> deleteDescItems;

    /**
     * Identifikátor verze.
     */
    private Integer versionId;

    /**
     * Typ atrubutu.
     */
    private RulDescItemType type;

    /**
     * Akce pro přidání dalšího atributu.
     */
    private AxAction newValueButton;

    /**
     * Loader hodnot atrubutu.
     */
    private AttributeValuesLoader attributeValuesLoader;

    /**
     * Seznam typů kalendárů.
     */
    private List<ArrCalendarType> calendarTypes;

    /**
     * Konstruktor pro nový atribut.
     * @param itemExtList   seznam hodnot atrubutu
     * @param descItemSpecs seznam specifikací
     * @param type          typ atributu
     * @param dataType      datový typ
     * @param node          uzel
     * @param versionId     identifikátor verze
     * @param attributeValuesLoader loader hodnot atributu
     * @param calendarTypes seznam kalendářů
     */
    public Attribut(List<ArrDescItem> itemExtList,
                    List<RulDescItemSpec> descItemSpecs,
                    RulDescItemType type,
                    RulDataType dataType,
                    ArrNode node,
                    Integer versionId,
                    final AttributeValuesLoader attributeValuesLoader,
                    List<ArrCalendarType> calendarTypes) {
        this.descItemSpecs = descItemSpecs;
        this.dataType = dataType;
        this.node = node;
        this.versionId = versionId;
        this.type = type;
        this.calendarTypes = calendarTypes;
        this.attributeValuesLoader = attributeValuesLoader;
        deleteDescItems = new ArrayList<>();
        addStyleName("table-hierarchy");

        Label nadpis = new Label(type.getName());
        nadpis.setSizeUndefined();
        nadpis.addStyleName("label-name");

        addComponent(nadpis);
        
        if (dataType.getCode().equalsIgnoreCase("PACKET_REF")) {
            addComponent(cssLayout("", createPacketButton()));
        }

        childs = new ChildComponentContainer<>();
        childs.addStyleName("container-values");

        addComponent(childs);
        itemExtList.sort(new AttributeValuesComparator());

        for (ArrDescItem descItemExt : itemExtList) {
            newAtributValue(descItemExt);
        }

        newValueButton = new AxAction().run(() -> {
            ArrDescItem descItem = createDescItemByType(dataType);
            descItem.setDescItemType(type);
            newAtributValue(descItem);
        }).caption("Přidat další hodnotu").icon(FontAwesome.PLUS);

        addComponent(cssLayout("", newValueButton.button()));

        // přidá nový řádek, pokud nebyl žádný (ulehčení pro uživatele)
        if (itemExtList.size() == 0) {
            ArrDescItem descItem = createDescItemByType(dataType);
            descItem.setDescItemType(type);
            newAtributValue(descItem);
        }
    }


    /**
     * Vrací seznam hodnot atributů
     * @return  seznam hodnot atributů
     */
    public List<ArrDescItem> getKeys() {
        List<ArrDescItem> collect = childs.getChils().stream().map(AttributValue::commit).collect(Collectors.toList());
        return collect;
    }

    /**
     * Obnovení hodnot atributů
     */
    public void revert() {
        childs.getChils().forEach(components1 -> components1.revert());
    }

    /**
     * Vrací uzel
     * @return uzel
     */
    public ArrNode getNode() {
        return node;
    }

    /**
     * Vytvoření hodnoty atributu podle datového typu
     * @param dataType  datový typ
     * @return  vytvořená hodnota atributu
     */
    public ArrDescItem createDescItemByType(RulDataType dataType) {
        Assert.notNull(dataType);

        switch (dataType.getCode()) {
            case "INT":
                return new ArrDescItemInt();
            case "STRING":
                return new ArrDescItemString();
            case "TEXT":
                return new ArrDescItemText();
            case "UNITDATE":
                return new ArrDescItemUnitdate();
            case "UNITID":
                return new ArrDescItemUnitid();
            case "FORMATTED_TEXT":
                return new ArrDescItemFormattedText();
            case "COORDINATES":
                return new ArrDescItemCoordinates();
            case "PARTY_REF":
                return new ArrDescItemPartyRef();
            case "RECORD_REF":
                return new ArrDescItemRecordRef();
            case "DECIMAL":
                return new ArrDescItemDecimal();
            case "PACKET_REF":
                return new ArrDescItemPacketRef();
            case "ENUM":
                return new ArrDescItemEnum();
            default:
                throw new NotImplementedException("Nebyl namapován datový typ");
        }
    }

    /**
     * Vytvoření gui objektu pro hodnotu atributu.
     * @param a hodnota atributu
     * @return  gui objekt
     */
    public AttributValue newAtributValue(final ArrDescItem a) {

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

        AttributValue value = new AttributValue(a, descItemSpecs, dataType, AxAction.of(a).icon(FontAwesome.TRASH_O).action(this::deleteAtributValue), wrapper, attributeValuesLoader, calendarTypes);

        a.setPosition(childs.getComponentCount() + 1);
        a.setNode(node);
        childs.addComponent(a, value);
        precislujPoradi();
        return value;
    }

    /**
     * Smazání gui objektu podle hodnoty atributu
     * @param a hodnota atributu
     */
    private void deleteAtributValue(final ArrDescItem a) {
        childs.removeComponent(a);
        if (a.getDescItemObjectId() != null) {
            deleteDescItems.add(a);
        }
        precislujPoradi();
    }

    /**
     * Vrací smazané hodnoty atributu
     * @return smazané hodnoty atributu
     */
    public List<ArrDescItem> getDeleteDescItems() {
        return deleteDescItems;
    }

    /**
     * Provede přečíslování pořadí u hodnot atributů
     */
    public void precislujPoradi() {
        Map<RulDescItemSpec, Integer> specIntegerMap = new HashMap<>();
        for (int i = 0; i < childs.getComponentCount(); i++) {
            AttributValue av = (AttributValue) childs.getComponent(i);
            ArrDescItem descItem = childs.getKey(av);

            if (descItem.getDescItemSpec() != null) {
                Integer position = specIntegerMap.get(descItem.getDescItemSpec());
                if (position == null) {
                    position = 1;
                } else {
                    position++;
                }
                descItem.setPosition(position);
                specIntegerMap.put(descItem.getDescItemSpec(), position);
            } else {
                descItem.setPosition(i + 1);
            }
        }
    }

    /**
     * Vrací identifikátor verze
     * @return  identifikátor verze
     */
    public Integer getVersionId() {
        return versionId;
    }

    private void createFormularPacket() {
        AxForm<ArrPacket> form = AxForm.init(ArrPacket.class);
        form.addStyleName("form");
        AxItemContainer<RulPacketType> container = AxItemContainer.init(RulPacketType.class);
        container.addAll(getPacketTypes());
        form.addCombo("Typ obalu", "packetType", container, "name");
        form.addField("Číslo", "storageNumber").required();
        form.addField("Zneplatněný", "invalidPacket");

        new AxWindow().caption("Vytvoření obalu").components(form).buttonClose().buttonPrimary(
                new AxAction<ArrPacket>().caption("Uložit")
                        .value(form::commit).action(this::createPacket)
                        .exception(ex -> {
                            ex.printStackTrace();
                            String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                            ElzaNotifications.showError(message);
                        })
                        ).modal().style("window-detail").show();
    }

    private void createPacket(final ArrPacket packet) {
        createPacket(packet, versionId);
    }

    protected abstract void createPacket(ArrPacket packet, Integer versionId);

    protected abstract List<RulPacketType> getPacketTypes();

    private Button createPacketButton() {
        Button button = new Button(FontAwesome.PLUS);
        button.setCaption("Vytvořit obal");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                createFormularPacket();
            }
        });

        return button;
    }
}
