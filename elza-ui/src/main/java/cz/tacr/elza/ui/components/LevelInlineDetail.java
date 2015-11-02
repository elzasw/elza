package cz.tacr.elza.ui.components;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.api.controller.PartyManager;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RegistryManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrLevelExt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.ArrDescItemSavePack;
import cz.tacr.elza.ui.components.attribute.Attribut;
import cz.tacr.elza.ui.components.attribute.AttributeValuesComparator;
import cz.tacr.elza.ui.components.attribute.AttributeValuesLoader;
import cz.tacr.elza.ui.components.attribute.NodeRegisterLink;
import cz.tacr.elza.ui.components.autocomplete.AutocompleteItem;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;
import cz.tacr.elza.ui.utils.ElzaNotifications;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


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
    private NodeRegisterLink nodeRegisterLink;
    private AxWindow attributWindow = null;
    private ComboBox attributesComboBox;
    private Label lblTitle;

    private Callback<ArrLevelExt> attributeEditCallback;



    public LevelInlineDetail() {
    }


    private void saveAttributeWithVersion(final Attribut attribut) {
        try {
            saveAttribute(attribut, true);
        } catch (final ConcurrentUpdateException e) {
            throw e;
        } catch (final RuntimeException e) {
            ElzaNotifications.showError(e.getMessage());
        }
    }

    private void saveAttributeWithoutVersion(final Attribut attribut) {
        try {
            saveAttribute(attribut, false);
        } catch (final ConcurrentUpdateException e) {
            throw e;
        } catch (final RuntimeException e) {
            ElzaNotifications.showError(e.getMessage());
        }
    }

    private void saveNodeRegisterLinkWithVersion(final NodeRegisterLink nodeRegisterLink) {
        try {

            List<ArrNodeRegister> links = nodeRegisterLink.getKeys();
            List<ArrNodeRegister> linksToDelete = nodeRegisterLink.getLinksToDelete();

            arrangementManager.modifyArrNodeRegisterLinks(links, linksToDelete);

            // obnova atributu vpravo
            ArrLevelExt level = arrangementManager.getLevel(nodeRegisterLink.getNode().getNodeId(),
                                                            nodeRegisterLink.getVersionId(), null);
            showLevelDetail(level, level.getDescItemList(), nodeRegisterLink.getVersionId(), attributeEditCallback);
            showNodeRegisterLink(nodeRegisterLink.getVersionId(), level.getNode());

            if (attributWindow != null) {
                attributWindow.close();
            }

        } catch (final ConcurrentUpdateException e) {
            throw e;
        } catch (final RuntimeException e) {
            ElzaNotifications.showError(e.getMessage());
        }
    }

    private void saveAttribute(final Attribut attribut, final Boolean createNewVersion) {
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
            showNodeRegisterLink(attribut.getVersionId(), level.getNode());
            sendEditCallback(level);
            if (attributWindow != null) {
                attributWindow.close();
            }
        } catch (final Exception e) {
            attribut.revert();
            throw e;
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
        for (final ArrDescItem item : descItemList) {
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
                    captionLayout.addComponent(createEditButton(
                            (thiz) -> thiz.showEditAttrWindow(level, item.getDescItemType(), versionId)));
                }

                Label lblValue = newLabel(value);
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(captionLayout, lblValue);


                lastDescItemTypeId = item.getDescItemType().getDescItemTypeId();
            }
        }



        detailContent.addComponent(grid);
    }

    public void showNodeRegisterLink(final Integer versionId, final ArrNode node) {

        List<ArrNodeRegister> data
                = arrangementManager.findNodeRegisterLinks(versionId, node.getNodeId());

        List<RegRecord> records = data.stream().map(ArrNodeRegister::getRecord).collect(Collectors.toList());

        FormGrid grid = new FormGrid().setRowSpacing(true).style("attr-detail");

        CssLayout captionLayout = cssLayoutExt(null);
        captionLayout.addComponent(newLabel("Vazba na rejstříkové heslo"));
        if (isVersionOpen(versionId)) {
            captionLayout.addComponent(createEditButton(
                    (thiz) -> thiz.showEditNodeRecordLinkWindow(node, versionId, data)));
        }

        if (CollectionUtils.isEmpty(records)) {
            grid.addRow(captionLayout, newLabel(""));
        }

        boolean first = true;
        for (final RegRecord record : records) {

            if (first) {

                Label lblValue = newLabel(record.getRecord());
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(captionLayout, lblValue);

                first = false;

            } else {

                Label lblValue = newLabel(record.getRecord(), "multi-value");
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow("", lblValue);

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

        for (final ArrDescItem descItem : descItems) {
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
            List<ArrCalendarType> calendarTypes = arrangementManager.getCalendarTypes().getCalendarTypes();
            RulDataType dataType = ruleSetManager.getDataTypeForDescItemType(type);
            attribut = new Attribut(listItem, listSpec, type, dataType, level.getNode(), versionId, getAttributeValuesLoader(), calendarTypes);

            try {

                attributWindow = new AxWindow();
                attributWindow.caption("Detail atributu")
                        .components(attribut)
                        .buttonClose()
                        .modal()
                        .style("window-detail").closeListener(e -> {
                    attributesComboBox.setValue(null);
                }).menuActions(AxAction.of(attribut).caption("Uložit").action(this::saveAttributeWithVersion)
                                .exception(new ConcurrentUpdateExceptionHandler()),
                        AxAction.of(attribut).caption("Uložit bez uložení historie")
                                .action(this::saveAttributeWithoutVersion).exception(new ConcurrentUpdateExceptionHandler())
                );

            } catch (final Exception e) {
                ElzaNotifications.showError(e.getMessage());
            }

            attributWindow.getWindow().center();
            UI.getCurrent().addWindow(attributWindow.getWindow());

        }
    }

    private void showEditNodeRecordLinkWindow(final ArrNode node, final Integer versionId, final List<ArrNodeRegister> data) {

        nodeRegisterLink = new NodeRegisterLink(node, versionId, data, registryManager);

        try {

            attributWindow = new AxWindow();
            attributWindow.caption("Detail vazeb na rejstřík")
                    .components(nodeRegisterLink)
                    .buttonClose()
                    .modal()
                    .style("window-detail").closeListener(e -> {
                attributesComboBox.setValue(null);
            }).menuActions(AxAction.of(nodeRegisterLink).caption("Uložit").action(this::saveNodeRegisterLinkWithVersion)
                    .exception(new ConcurrentUpdateExceptionHandler())
            );

        } catch (final Exception e) {
            ElzaNotifications.showError(e.getMessage());
        }

        attributWindow.getWindow().center();
        UI.getCurrent().addWindow(attributWindow.getWindow());
    }


    private Button createEditButton(final Consumer<LevelInlineDetail> function) {

        LevelInlineDetail thiz = this;

        Button button = new Button(FontAwesome.EDIT);
        button.addStyleName("edit-btn");
        button.addStyleName("icon-button");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                function.accept(thiz);
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

                    for (final ParParty partyItem : partyList) {
                        result.add(new AutocompleteItem(partyItem, partyItem.getRecord().getRecord()));
                    }

                    return result;
                }

                @Override
                public List<AutocompleteItem> loadRecordRefItemsFulltext(final String text, final RulDescItemSpec specification) {
                    if(specification == null){
                        return Collections.EMPTY_LIST;
                    }

                    Set<Integer> regTypeIdSet = new HashSet<>();
                    List<RegRegisterType> registerTypeList = registryManager.getRegisterTypesForDescItemSpec(specification.getDescItemSpecId());
                    for (final RegRegisterType regRegisterType : registerTypeList) {
                        regTypeIdSet.add(regRegisterType.getRegisterTypeId());
                    }

                    List<RegRecord> recordList = registryManager
                            .findRecord(text, 0, 50, regTypeIdSet.toArray(new Integer[regTypeIdSet.size()]));
                    List<AutocompleteItem> result = new ArrayList<>(recordList.size());

                    for (final RegRecord regRecord : recordList) {
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
