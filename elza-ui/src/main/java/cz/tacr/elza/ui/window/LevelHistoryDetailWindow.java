package cz.tacr.elza.ui.window;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.vo.ArrNodeHistoryItem;
import cz.tacr.elza.ui.components.FormGrid;
import cz.tacr.elza.ui.components.attribute.AttributeValuesComparator;


/**
 * @author Martin Šlapa
 * @since 22.9.2015
 */
public class LevelHistoryDetailWindow extends AxWindow {

    private ArrangementManager arrangementManager;

    public LevelHistoryDetailWindow(final ArrangementManager arrangementManager) {
        Assert.notNull(arrangementManager);

        this.arrangementManager = arrangementManager;
    }

    public AxWindow show(final ArrNodeHistoryItem nodeHistoryItem) {
        CssLayout layout = new CssLayout();
        layout.addStyleName("history-box");
        layout.setSizeUndefined();

        switch (nodeHistoryItem.getType()) {
            case ATTRIBUTE_CHANGE:
                layout.addComponent(layoutAttributes(nodeHistoryItem));
                break;
            default:
                throw new IllegalStateException("Neplatný vstupní typ");
        }

        caption("Detail změny").components(layout).buttonClose().modal().style("history - window - detail");
        return super.show();
    }

    public Component layoutAttributes(ArrNodeHistoryItem nodeHistoryItem) {
        CssLayout layout = new CssLayout();
        List<ArrDescItem> descItems = nodeHistoryItem.getDescItems();
        List<ArrDescItem> descItemsAfter = new ArrayList<>();
        List<ArrDescItem> descItemsBefore = new ArrayList<>();
        for (ArrDescItem descItem : descItems) {
            if (descItem.getCreateChange().equals(nodeHistoryItem.getChange()) && !nodeHistoryItem.getChange().equals(descItem.getDeleteChange())) {
                descItemsAfter.add(descItem);
            }
            if (!nodeHistoryItem.getChange().equals(descItem.getCreateChange()) && nodeHistoryItem.getChange().equals(descItem.getDeleteChange())) {
                descItemsBefore.add(descItem);
            }
        }

        descItemsAfter.sort(new AttributeValuesComparator());
        descItemsBefore.sort(new AttributeValuesComparator());

        FormGrid formGridBefore = createAttributeGrid(descItemsBefore);
        CssLayout layoutBefore = new CssLayout();
        layoutBefore.addStyleName("change-attribute-before");
        layoutBefore.addComponent(newLabel("Před změnou"));
        layoutBefore.addComponent(formGridBefore);
        layout.addComponent(layoutBefore);

        FormGrid formGridAfter = createAttributeGrid(descItemsAfter);
        CssLayout layoutAfter = new CssLayout();
        layoutAfter.addStyleName("change-attribute-after");
        layoutAfter.addComponent(newLabel("Po změně"));
        layoutAfter.addComponent(formGridAfter);
        layout.addComponent(layoutAfter);

        return layout;
    }

    private FormGrid createAttributeGrid(List<ArrDescItem> descItems) {
        FormGrid grid = new FormGrid().setRowSpacing(true).style("attr-detail");
        grid.setMarginTop(true);
        Integer lastDescItemTypeId = null;
        for (ArrDescItem item : descItems) {
            String caption;
            String value = item.toString();

            if (item.getDescItemSpec() != null) {
                String specName = item.getDescItemSpec().getName();
                value = specName + ": " + value;
            }

            if (item.getDescItemType().getDescItemTypeId().equals(lastDescItemTypeId)) {
                caption = "";
                Label lblValue = newLabel(value, "multi-value");
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(caption, lblValue);
            } else {
                caption = item.getDescItemType().getName();

                CssLayout captionLayout = new CssLayout();
                captionLayout.addComponent(newLabel(caption));


                Label lblValue = newLabel(value);
                lblValue.setContentMode(ContentMode.HTML);
                grid.addRow(captionLayout, lblValue);


                lastDescItemTypeId = item.getDescItemType().getDescItemTypeId();
            }
        }

        return grid;
    }

}
