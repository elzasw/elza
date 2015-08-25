package cz.tacr.elza.ui.components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Table;

import cz.req.ax.AxAction;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.8.2015
 */
public class LevelInlineDetail extends CssLayout implements Components {

    private Runnable onClose;
    private CssLayout detailContent;

    public LevelInlineDetail(final Runnable onClose) {
        setSizeUndefined();
        addStyleName("level-detail");
        addStyleName("hidden");
        this.onClose = onClose;

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


    public void showLevelDetail(final ArrFaLevel level, final List<ArrDescItemExt> descItemList) {
        removeStyleName("hidden");
        detailContent.removeAllComponents();
        detailContent.addComponent(newLabel("Zobrazen level s nodeId "+level.getNodeId()));
        detailContent.addComponent(newLabel("Pozice: "+level.getPosition()));

        Collections.sort(descItemList, new Comparator<ArrDescItemExt>() {

            @Override
            public int compare(ArrDescItemExt o1, ArrDescItemExt o2) {
                Integer specOrder1 = (o1.getDescItemSpec() == null) ? null : o1.getDescItemSpec().getViewOrder();
                Integer specOrder2 = (o2.getDescItemSpec() == null) ? null : o2.getDescItemSpec().getViewOrder();
                return new CompareToBuilder()
                        .append(o1.getDescItemType().getViewOrder(), o2.getDescItemType().getViewOrder())
                        .append(o1.getDescItemType().getName(), o2.getDescItemType().getName())
                        .append(specOrder1, specOrder2)
                        .append(o1.getPosition(), o2.getPosition())
                        .toComparison();
            }
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
        table.addGeneratedColumn("data", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrDescItemExt descItem = (ArrDescItemExt) itemId;
                String specName = null;
                if (descItem.getDescItemSpec() != null) {
                    specName = descItem.getDescItemSpec().getName();
                }
                return StringUtils.defaultString(specName) + " " + descItem.getData();
            }
        });
        BeanItemContainer<ArrDescItemExt> container = new BeanItemContainer<>(ArrDescItemExt.class);
        container.addAll(descItemList);
        table.addStyleName("table");
        table.setContainerDataSource(container);
        table.setVisibleColumns("descItemType", "data");
        detailContent.addComponent(table);
    }



}
