package cz.tacr.elza.ui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.api.controller.PartyManager;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RegistryManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.ui.components.attribute.Attribut;
import cz.tacr.elza.ui.components.attribute.AttributeValuesComparator;
import cz.tacr.elza.ui.components.attribute.AttributeValuesLoader;
import cz.tacr.elza.ui.components.autocomplete.AutocompleteItem;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.8.2015
 */

@Component
@Scope("prototype")
public class LevelInlineDetail extends CssLayout implements Components, InitializingBean {

    public static final String TITLE_CODE = "ZP2015_TITLE";

    private CssLayout detailContent;

    @Autowired
    private RuleManager ruleSetManager;

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private PartyManager partyManager;

    @Autowired
    private RegistryManager registryManager;


    private AttributeValuesLoader attributeValuesLoader;

    private Attribut attribut;
    private AxWindow attributWindow = null;
    private ComboBox attributesComboBox;
    private Label lblTitle;

    private Callback<ArrLevelExt> attributeEditCallback;



    public LevelInlineDetail() {
    }


    private void saveAttributeWithVersion(Attribut attribut) {
        saveAttribute(attribut, true);
    }

    private void saveAttributeWithoutVersion(Attribut attribut) {
        saveAttribute(attribut, false);
    }

    private void saveAttribute(Attribut attribut, Boolean createNewVersion) {
        List<ArrDescItem> descItems = attribut.getKeys();
        List<ArrDescItem> deleteDescItems = attribut.getDeleteDescItems();

        ArrDescItemSavePack pack = new ArrDescItemSavePack();

        pack.setDescItems(descItems);
        pack.setDeleteDescItems(deleteDescItems);
        pack.setCreateNewVersion(createNewVersion);
        pack.setFaVersionId(attribut.getVersionId());
        pack.setNode(attribut.getNode());

        try {
            arrangementManager.saveDescriptionItems(pack);
            ArrLevelExt level = arrangementManager.getLevel(attribut.getNode().getNodeId(), attribut.getVersionId(), null);
            showLevelDetail(level, level.getDescItemList(), attribut.getVersionId(), attributeEditCallback);
            sendEditCallback(level);
            if (attributWindow != null) {
                attributWindow.close();
            }
        } catch (Exception e) {
            attribut.revert();
            Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }


    public void showLevelDetail(final ArrLevelExt level,
                                final List<ArrDescItem> descItemList,
                                final Integer versionId, final Callback<ArrLevelExt> attributeEditCallback) {
        this.attributeEditCallback = attributeEditCallback;
        detailContent.removeAllComponents();
        initContentTitle(level, descItemList);

        boolean versionOpen = isVersionOpen(versionId);

        if (versionOpen) {
            BeanItemContainer<RulDescItemType> descItemTypeBeanItemContainer = new BeanItemContainer<>(
                    RulDescItemType.class);
            descItemTypeBeanItemContainer.addAll(ruleSetManager
                    .getDescriptionItemTypesForNodeId(versionId, level.getNode().getNodeId(), null));
            attributesComboBox = new ComboBox(null, descItemTypeBeanItemContainer);
            attributesComboBox.setItemCaptionPropertyId("name");

            attributesComboBox.addValueChangeListener(event -> {
                RulDescItemType type = (RulDescItemType) event.getProperty().getValue();
                showEditAttrWindow(level, type, versionId);
            });
            detailContent.addComponent(attributesComboBox);
        }

        descItemList.sort(new AttributeValuesComparator());

        FormGrid grid = new FormGrid().setRowSpacing(true).style("attr-detail");
        grid.setMarginTop(true);
        Integer lastDescItemTypeId = null;
        for (ArrDescItem item : descItemList) {
            String caption;
            String value = item.toString();

            if (item.getDescItemSpec() != null) {
                String specName = item.getDescItemSpec().getName();
                value = specName + ": " + value;
            }

            if(item.getDescItemType().getDescItemTypeId().equals(lastDescItemTypeId)){
                caption = "";
                Label lblValue = newLabel(value, "multi-value");
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(caption, lblValue);
            }else{
                caption = item.getDescItemType().getName();

                CssLayout captionLayout = cssLayoutExt(null);
                captionLayout.addComponent(newLabel(caption));
                if(versionOpen) {
                    captionLayout.addComponent(createEditButton(level, item.getDescItemType(), versionId));
                }

                Label lblValue = newLabel(value);
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(captionLayout, lblValue);


                lastDescItemTypeId = item.getDescItemType().getDescItemTypeId();
            }
        }

        detailContent.addComponent(grid);
    }

    private void sendEditCallback(final ArrLevelExt level){
        if(attributeEditCallback != null){
            attributeEditCallback.callback(level);
        }
    }

    private boolean isVersionOpen(final Integer versionId) {
        Assert.notNull(versionId);

        return arrangementManager.getFaVersionById(versionId).getLockChange() == null;
    }

    public void initContentTitle(final ArrLevelExt level, final List<ArrDescItem> descItems){
        Assert.notNull(descItems);

        lblTitle.setValue("Detail archivního popisu s id=" + level.getNode().getNodeId());

        for (ArrDescItem descItem : descItems) {
            if (descItem.getDescItemType().getCode().equals(TITLE_CODE)) {
                ArrDescItemString descItemString = (ArrDescItemString) descItem;
                if (StringUtils.isNotBlank(descItemString.getValue()))
                lblTitle.setValue(descItemString.getValue());
                break;
            }
        }
    }



    private void showEditAttrWindow(final ArrLevelExt level, final RulDescItemType type, final Integer versionId) {
        if (type != null) {
            List<ArrDescItem> listItem = arrangementManager
                    .getDescriptionItemsForAttribute(versionId, level.getNode().getNodeId(), type.getDescItemTypeId());
            List<RulDescItemSpec> listSpec = ruleSetManager.getDescItemSpecsFortDescItemType(type);
            RulDataType dataType = ruleSetManager.getDataTypeForDescItemType(type);
            attribut = new Attribut(listItem, listSpec, type, dataType, level.getNode(), versionId, getAttributeValuesLoader());

            attributWindow = new AxWindow();
            attributWindow.caption("Detail atributu")
                    .components(attribut)
                    .buttonClose()
                    .modal()
                    .style("window-detail").closeListener(e -> {
                attributesComboBox.setValue(null);
            }).menuActions(AxAction.of(attribut).caption("Uložit").action(this::saveAttributeWithVersion),
                    AxAction.of(attribut).caption("Uložit bez uložení historie")
                            .action(this::saveAttributeWithoutVersion)
            );

            attributWindow.getWindow().center();
            UI.getCurrent().addWindow(attributWindow.getWindow());

        }
    }


    private Button createEditButton(final ArrLevelExt level, final RulDescItemType type, final Integer versionId) {

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


    private AttributeValuesLoader getAttributeValuesLoader() {
        if (attributeValuesLoader == null) {
            attributeValuesLoader = new AttributeValuesLoader() {
                @Override
                public List<AutocompleteItem> loadPartyRefItemsFulltext(final String text) {
                    List<ParParty> partyList = partyManager.findParty(text, 0, 50, null, true);
                    List<AutocompleteItem> result = new ArrayList<>(partyList.size());

                    for (ParParty partyItem : partyList) {
                        result.add(new AutocompleteItem(partyItem, partyItem.getRecord().getRecord()));
                    }

                    return result;
                }

                @Override
                public List<AutocompleteItem> loadRecordRefItemsFulltext(final String text, final RulDescItemSpec specification) {
                    if(specification == null){
                        return Collections.EMPTY_LIST;
                    }

                    RulDescItemSpec specDo = ruleSetManager.getDescItemSpecById(specification.getDescItemSpecId());

                    List<RegRecord> recordList = registryManager
                            .findRecord(text, 0, 50, specDo.getRegisterType().getRegisterTypeId());
                    List<AutocompleteItem> result = new ArrayList<>(recordList.size());

                    for (RegRecord regRecord : recordList) {
                        result.add(new AutocompleteItem(regRecord, regRecord.getRecord()));
                    }

                    return result;
                }
            };
        }
        return attributeValuesLoader;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setSizeUndefined();
        addStyleName("level-detail");
        detailContent = cssLayoutExt("detail-content");

        lblTitle = newLabel("Detail archivního popisu", "h2 text-ellipsis");
        addComponent(lblTitle);
        addComponent(detailContent);
    }
}
