package cz.tacr.elza.ui.window;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.util.Assert;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.And;
import com.vaadin.event.dd.acceptcriteria.SourceIs;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableDragMode;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.vo.FaViewDescItemTypes;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.8.2015
 */

public class DescItemTypeWindow extends AxWindow {
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_SELECTED = "SELECTED";
    private static final String COLUMN_NAME = "NAME";

    private RuleManager ruleSetManager;

    private IndexedContainer container;
    private Integer ruleSetId;
    private Integer arrangementTypeId;
    private RulFaView rulFaView;

    public DescItemTypeWindow(final RuleManager ruleSetManager) {
        Assert.notNull(ruleSetManager);
        this.ruleSetManager = ruleSetManager;
    }

    public AxWindow show(final ArrFaVersion version, PosAction posAction) {
        ruleSetId = version.getRuleSet().getRuleSetId();
        arrangementTypeId = version.getArrangementType().getArrangementTypeId();
        List<RulDescItemTypeExt> itemTypeSet = ruleSetManager.getDescriptionItemTypes(ruleSetId);

        FaViewDescItemTypes faViewDescItemTypes = ruleSetManager.getFaViewDescItemTypes(version.getFaVersionId());
        rulFaView = faViewDescItemTypes.getRulFaView();
        List<RulDescItemType> selectedItemTypeList = faViewDescItemTypes.getDescItemTypes();
        List<Integer> selectedItemTypeIdList = new LinkedList<>();
        for (RulDescItemType itemTypeIdStr : selectedItemTypeList) {
            selectedItemTypeIdList.add(itemTypeIdStr.getDescItemTypeId());
        }

        final Integer maxPosition = selectedItemTypeIdList.size();
        Collections.sort(itemTypeSet, new Comparator<RulDescItemType>() {

            @Override
            public int compare(RulDescItemType r1, RulDescItemType r2) {
                Integer position1 = selectedItemTypeIdList.indexOf(r1.getDescItemTypeId());
                Integer position2 = selectedItemTypeIdList.indexOf(r2.getDescItemTypeId());
                if (position1 < 0) {
                    position1 = maxPosition;
                }
                if (position2 < 0) {
                    position2 = maxPosition;
                }
                return position1.compareTo(position2);
            }

        });

        final Table table = new Table();
        table.setWidth("100%");
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        container = new IndexedContainer();
        container.addContainerProperty(COLUMN_ID, Integer.class, 0);
        container.addContainerProperty(COLUMN_SELECTED, Boolean.class, Boolean.FALSE);
        container.addContainerProperty(COLUMN_NAME, String.class, "");
        for (RulDescItemTypeExt rulDescItemTypeExt : itemTypeSet) {
            Item newItem = container.getItem(container.addItem());
            Integer itemTypeId = rulDescItemTypeExt.getDescItemTypeId();
            setItemData(newItem, selectedItemTypeIdList.contains(itemTypeId), itemTypeId,
                    rulDescItemTypeExt.getName());
        }
        table.addGeneratedColumn(COLUMN_SELECTED, new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId,
                    final Object columnId) {
                Boolean checked =
                        (Boolean) source.getItem(itemId).getItemProperty(columnId).getValue();
                CheckBox checkBox = new CheckBox();
                checkBox.setValue(checked);
                checkBox.addValueChangeListener(new Property.ValueChangeListener() {
                    @Override
                    public void valueChange(Property.ValueChangeEvent valueChangeEvent) {

                        Boolean selected = (Boolean) valueChangeEvent.getProperty().getValue();
                        updateCheckBox(itemId, selected);
                    }
                });
                return checkBox;
            }
        });

        table.setDragMode(TableDragMode.ROW);
        table.setDropHandler(createDropHandler(table));

        table.addStyleName("table");
        table.setContainerDataSource(container);
        table.setVisibleColumns(COLUMN_SELECTED, COLUMN_NAME);

        caption("Výber sloupců k zobrazení").components(table).buttonClose("Storno")
                .buttonPrimary(new AxAction().caption("Uložit").exception(new ConcurrentUpdateExceptionHandler())
                .run(() -> save(posAction))).modal().style("window-detail");

        return super.show();
    }

    private void setItemData(Item targer, Boolean selected, Integer id, String name) {
        targer.getItemProperty(COLUMN_SELECTED).setValue(selected);
        targer.getItemProperty(COLUMN_ID).setValue(id);
        targer.getItemProperty(COLUMN_NAME).setValue(name);
    }

    private void updateCheckBox(Object itemId, Boolean value) {
        Item targer = container.getItem(itemId);
        targer.getItemProperty(COLUMN_SELECTED).setValue(value);
    }

    private void save(PosAction posAction) {
        List<Integer> resultList = new LinkedList<>();
        for (Object itemId : container.getItemIds()) {
            Item item = container.getItem(itemId);
            Boolean selected = (Boolean) item.getItemProperty(COLUMN_SELECTED).getValue();
            if (BooleanUtils.isTrue(selected)) {
                Integer id = (Integer) item.getItemProperty(COLUMN_ID).getValue();
                resultList.add(id);
            }
        }
        ruleSetManager.saveFaViewDescItemTypes(rulFaView,
                resultList.toArray(new Integer[resultList.size()]));
        posAction.onCommit();
    }

    private DropHandler createDropHandler(final Table table) {
        return new DropHandler() {
            @Override
            public void drop(DragAndDropEvent dropEvent) {
                DataBoundTransferable t = (DataBoundTransferable) dropEvent.getTransferable();
                Object sourceItemId = t.getItemId();

                AbstractSelectTargetDetails dropData =
                        ((AbstractSelectTargetDetails) dropEvent.getTargetDetails());
                Object targetItemId = dropData.getItemIdOver();

                if (sourceItemId == targetItemId || targetItemId == null) return;

                Item prevItem = container.getItem(sourceItemId);
                Boolean selected = (Boolean) prevItem.getItemProperty(COLUMN_SELECTED).getValue();
                Integer id = (Integer) prevItem.getItemProperty(COLUMN_ID).getValue();
                String name = (String) prevItem.getItemProperty(COLUMN_NAME).getValue();
                container.removeItem(sourceItemId);

                if (dropData.getDropLocation() == VerticalDropLocation.BOTTOM) {
                    Item newItem = container.addItemAfter(targetItemId, sourceItemId);
                    setItemData(newItem, selected, id, name);
                } else {
                    Object prevItemId = container.prevItemId(targetItemId);
                    Item newItem = container.addItemAfter(prevItemId, sourceItemId);
                    setItemData(newItem, selected, id, name);
                }
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return new And(new SourceIs(table), AcceptItem.ALL);
            }
        };
    }
}
