package cz.tacr.elza.ax;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxContainer;
import cz.tacr.elza.ax.AxForm;
import cz.tacr.elza.ax.AxTable;


public interface InitContainerTableForm<S extends cz.tacr.elza.ax.IdObject<Integer>> {

    void init(AxContainer<S> container, AxTable<S> table, AxForm<S> form);

}
