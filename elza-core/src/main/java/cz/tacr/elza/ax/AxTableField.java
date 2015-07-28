package cz.tacr.elza.ax;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxForm;
import cz.tacr.elza.ax.AxItemTable;


public class AxTableField {

    cz.tacr.elza.ax.AxItemTable table;
    cz.tacr.elza.ax.AxForm form;

    public AxTableField(Class clazz, InitTableForm init) {
        cz.tacr.elza.ax.AxItemContainer container = cz.tacr.elza.ax.AxItemContainer.init(clazz);
        table = AxItemTable.init(container);
        form = AxForm.init(clazz);
        init.init(table, form);
    }
}
