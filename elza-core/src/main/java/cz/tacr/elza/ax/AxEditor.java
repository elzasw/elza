package cz.tacr.elza.ax;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxAction;
import cz.tacr.elza.ax.AxBeanTable;
import cz.tacr.elza.ax.AxConfirm;


public class AxEditor<T extends IdObject<Integer>> extends CssLayout implements Refresh {

    cz.tacr.elza.ax.AxContainer<T> container;
    AxForm<T> form;
    AxTable<T> table;

    public static <T extends IdObject<Integer>> AxEditor<T> init(cz.tacr.elza.ax.AxContainer<T> cont) {
        return new AxEditor<T>(cont);
    }

    private AxEditor(final cz.tacr.elza.ax.AxContainer<T> cont) {
        container = cont;

        form = AxForm.init(container.getType());
        form.setSizeUndefined();
        form.getButtonBar().addComponent(new cz.tacr.elza.ax.AxAction<T>().caption("Uložit").primary()
                .run(() -> {
                    //TODO fix value??? .value(form.commit()).action(entity -> {
                    T entity = null;
                    try {
                        entity = form.commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (entity.getId() == null) {
                        form.setItem(container.create(entity));
                    } else {
                        form.setItem(container.update(entity));
                    }
                    Notification.show("Uloženo");
                    table.refresh();
                    table.getTable().select(form.getValue().getId());
                }).button());
        form.getButtonBar().addComponent(new cz.tacr.elza.ax.AxAction<T>().caption("Nový")
                .action(entity -> form.setItem(container.newItem())).button());
        form.getButtonBar().addComponent(new AxAction<T>().caption("Smazat")
                .action(entity -> {
                    if (form.getValue().getId() != null) {
                        new AxConfirm("Smazat vybranou položku?", () -> {
                            try {
                                container.delete(form.getValue());
                                form.setItem(container.newItem());
                            } catch (Exception e) {
                                new AxMessage("Nelze odstranit vybranou položku.").show();
                            }
                        }).show();
                    }
                }).button());

        table = new AxBeanTable<>(container);
        table.getTable().addValueChangeListener(event -> {
            Object value = event.getProperty().getValue();
            form.setItem(value == null ? container.newItem() : container.getItem(value));
        });
    }

    public AxEditor<T> initHorizontal(InitContainerTableForm<T> init) {
        init.init(container, table, form);
        table.done();
        table.getTable();
        setStyleName("editor-horizontal");
        setSizeUndefined();
        addComponents(table.getTable(), form);
        return this;
    }

    public AxEditor<T> initVertical(InitContainerTableForm<T> init) {
        init.init(container, table, form);
        table.done();
        table.getTable().setVisible(container.size() > 0);
        container.addItemSetChangeListener(e -> table.getTable().setVisible(e.getContainer().size() > 0));
        setStyleName("editor-vertical");
        setSizeUndefined();
        addComponents(table.getTable(), form);
        return this;
    }

    public void refresh() {
        table.refresh();
    }

}
