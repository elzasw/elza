package cz.tacr.elza.ui.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.req.ax.Components;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaLevelExt;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.8.2015
 */
public class LevelInlineDetail extends CssLayout implements Components {

    private Runnable onClose;
    private CssLayout detailContent;

    private RuleManager ruleSetManager;
    private ArrangementManager arrangementManager;

    private Attribut attribut;
    private AxWindow attributWindow = null;

    public LevelInlineDetail(final Runnable onClose, final RuleManager ruleSetManager, final ArrangementManager arrangementManager) {
        setSizeUndefined();
        addStyleName("level-detail");
        addStyleName("hidden");
        this.onClose = onClose;
        this.ruleSetManager = ruleSetManager;
        this.arrangementManager = arrangementManager;

        init();
    }

    private void init(){
        Button closeButton = new AxAction().icon(FontAwesome.TIMES).right().run(()->{
            LevelInlineDetail.this.addStyleName("hidden");
            onClose.run();
        }).button();
        detailContent = cssLayout("detail-content");

        addComponent(newLabel("Detail atributu", "h2"));
        addComponent(closeButton);
        addComponent(detailContent);
    }

    private void saveAttributeWithVersion(Attribut attribut) {
        saveAttribute(attribut, true);
    }

    private void saveAttributeWithoutVersion(Attribut attribut) {
        saveAttribute(attribut, false);
    }

    private void saveAttribute(Attribut attribut, Boolean createNewVersion) {
        List<ArrDescItemExt> descItems = attribut.getKeys();
        List<ArrDescItemExt> deleteDescItems = attribut.getDeleteDescItems();

        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        pack.setDescItems(descItems);
        pack.setDeleteDescItems(deleteDescItems);
        pack.setCreateNewVersion(createNewVersion);
        pack.setFaVersionId(attribut.getVersionId());

        try {
            arrangementManager.saveDescriptionItems(pack);
            ArrFaLevelExt level = arrangementManager.getLevel(attribut.getNodeId(), attribut.getVersionId(), null);
            showLevelDetail(level, level.getDescItemList(), attribut.getVersionId());
            if (attributWindow != null) {
                attributWindow.close();
            }
        } catch (Exception e) {
            Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }

    public void showLevelDetail(final ArrFaLevelExt level, final List<ArrDescItemExt> descItemList, final Integer versionId) {
        removeStyleName("hidden");
        detailContent.removeAllComponents();
        detailContent.addComponent(newLabel("Zobrazen level s nodeId " + level.getNodeId()));
        detailContent.addComponent(newLabel("Pozice: "+level.getPosition()));

        BeanItemContainer<RulDescItemType> descItemTypeBeanItemContainer = new BeanItemContainer<>(RulDescItemType.class);
        descItemTypeBeanItemContainer.addAll(ruleSetManager.getDescriptionItemTypes(0));
        ComboBox attributesComboBox = new ComboBox(null, descItemTypeBeanItemContainer);
        attributesComboBox.setItemCaptionPropertyId("name");

        attributesComboBox.addValueChangeListener(event -> {
            RulDescItemType type = (RulDescItemType) event.getProperty().getValue();
            if (type != null) {
                List<ArrDescItemExt> listItem = ruleSetManager.getDescriptionItemsForAttribute(versionId, level.getNodeId(), type.getDescItemTypeId());
                List<RulDescItemSpec> listSpec = ruleSetManager.getDescItemSpecsFortDescItemType(type);
                RulDataType dataType = ruleSetManager.getDataTypeForDescItemType(type);
                attribut = new Attribut(listItem, listSpec, type, dataType, level.getNodeId(), versionId);

                attributWindow = new AxWindow();
                attributWindow.caption("Detail attributu")
                        .components(attribut)
                        .buttonClose()
                        .modal()
                        .style("window-detail").show().closeListener(e -> {
                    attributesComboBox.setValue(null);
                }).menuActions(AxAction.of(attribut).caption("Uložit").action(this::saveAttributeWithVersion), AxAction.of(attribut).caption("Uložit bez uložení historie").action(this::saveAttributeWithoutVersion));
            }
        });
        detailContent.addComponent(attributesComboBox);

        Collections.sort(descItemList, (o1, o2) -> {
            Integer specOrder1 = (o1.getDescItemSpec() == null) ? null : o1.getDescItemSpec().getViewOrder();
            Integer specOrder2 = (o2.getDescItemSpec() == null) ? null : o2.getDescItemSpec().getViewOrder();
            return new CompareToBuilder()
                    .append(o1.getDescItemType().getViewOrder(), o2.getDescItemType().getViewOrder())
                    .append(o1.getDescItemType().getName(), o2.getDescItemType().getName())
                    .append(specOrder1, specOrder2)
                    .append(o1.getPosition(), o2.getPosition())
                    .toComparison();
        });

        Table table = new Table();
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        table.setWidth("100%");
        table.addContainerProperty("descItemType", String.class, null, "Attribut", null, null);
        table.addContainerProperty("data", String.class, null, "Hodnota", null, null);
        final Set<Integer> useValue = new HashSet<>();
        table.addGeneratedColumn("descItemType", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrDescItemExt descItem = (ArrDescItemExt) itemId;
                Integer descItemTypeId = descItem.getDescItemType().getDescItemTypeId();
                if (useValue.contains(descItemTypeId)) {
                    return "";
                }
                useValue.add(descItemTypeId);
                return descItem.getDescItemType().getName();
            }
        });
        table.addGeneratedColumn("data", (source, itemId, columnId) -> {
            ArrDescItemExt descItem = (ArrDescItemExt) itemId;
            String specName = null;
            if (descItem.getDescItemSpec() != null) {
                specName = descItem.getDescItemSpec().getName();
            }
            return StringUtils.defaultString(specName) + " " + descItem.getData();
        });
        BeanItemContainer<ArrDescItemExt> container = new BeanItemContainer<>(ArrDescItemExt.class);
        container.addAll(descItemList);
        table.addStyleName("attribut-table");
        table.setContainerDataSource(container);
        table.setVisibleColumns("descItemType", "data");
        detailContent.addComponent(table);
    }



}
