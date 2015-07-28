package cz.tacr.elza.ax;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxForm;
import cz.tacr.elza.ax.AxTable;


public interface InitTableForm<S extends cz.tacr.elza.ax.IdObject<Integer>> {

    void init(AxTable<S> table, AxForm<S> form);

}
