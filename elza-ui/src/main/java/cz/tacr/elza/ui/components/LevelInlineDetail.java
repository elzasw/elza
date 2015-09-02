package cz.tacr.elza.ui.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.CompareToBuilder;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
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

    private CssLayout detailContent;

    private RuleManager ruleSetManager;
    private ArrangementManager arrangementManager;

    private Attribut attribut;
    private AxWindow attributWindow = null;
    private ComboBox attributesComboBox;

    public LevelInlineDetail(final RuleManager ruleSetManager, final ArrangementManager arrangementManager) {
        setSizeUndefined();
        addStyleName("level-detail");
        this.ruleSetManager = ruleSetManager;
        this.arrangementManager = arrangementManager;

        init();
    }

    private void init() {
        detailContent = cssLayoutExt("detail-content");

        addComponent(newLabel("Detail atributu", "h2"));
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

    public void showLevelDetail(final ArrFaLevelExt level,
                                final List<ArrDescItemExt> descItemList,
                                final Integer versionId) {
        detailContent.removeAllComponents();

        BeanItemContainer<RulDescItemType> descItemTypeBeanItemContainer = new BeanItemContainer<>(
                RulDescItemType.class);
        descItemTypeBeanItemContainer.addAll(ruleSetManager.getDescriptionItemTypes(0));
        attributesComboBox = new ComboBox(null, descItemTypeBeanItemContainer);
        attributesComboBox.setItemCaptionPropertyId("name");

        attributesComboBox.addValueChangeListener(event -> {
            RulDescItemType type = (RulDescItemType) event.getProperty().getValue();
            showEditAttrWindow(level, type, versionId);
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


        FormGrid grid = new FormGrid().setRowSpacing(true).style("attr-detail");
        grid.setMarginTop(true);
        Integer lastDescItemTypeId = null;
        for (ArrDescItemExt item : descItemList) {
            String caption;
            if(item.getDescItemType().getDescItemTypeId().equals(lastDescItemTypeId)){
                caption = "";
                grid.addRow(caption, newLabel(item.getData(), "multi-value"));
            }else{
                caption = item.getDescItemType().getName();

                CssLayout captionLayout = cssLayoutExt(null);
                captionLayout.addComponent(newLabel(caption));
                captionLayout.addComponent(createEditButton(level, item.getDescItemType(), versionId));

                CssLayout layout = grid.addRow(captionLayout, newLabel(item.getData()));


                lastDescItemTypeId = item.getDescItemType().getDescItemTypeId();
            }
        }

        detailContent.addComponent(grid);
    }


    private void showEditAttrWindow(final ArrFaLevelExt level, final RulDescItemType type, final Integer versionId) {
        if (type != null) {
            List<ArrDescItemExt> listItem = ruleSetManager
                    .getDescriptionItemsForAttribute(versionId, level.getNodeId(), type.getDescItemTypeId());
            List<RulDescItemSpec> listSpec = ruleSetManager.getDescItemSpecsFortDescItemType(type);
            RulDataType dataType = ruleSetManager.getDataTypeForDescItemType(type);
            attribut = new Attribut(listItem, listSpec, type, dataType, level.getNodeId(), versionId);

            attributWindow = new AxWindow();
            attributWindow.caption("Detail atributu")
                    .components(attribut)
                    .buttonClose()
                    .modal()
                    .style("window-detail").show().closeListener(e -> {
                attributesComboBox.setValue(null);
            }).menuActions(AxAction.of(attribut).caption("Uložit").action(this::saveAttributeWithVersion),
                    AxAction.of(attribut).caption("Uložit bez uložení historie")
                            .action(this::saveAttributeWithoutVersion)
            );
        }
    }


    private Button createEditButton(final ArrFaLevelExt level, final RulDescItemType type, final Integer versionId) {

        Button button = new Button(FontAwesome.EDIT);
        button.addStyleName("edit-btn");
        button.addStyleName("icon-button");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                showEditAttrWindow(level, type, versionId);

            }
        });


        return button;
    }
}
