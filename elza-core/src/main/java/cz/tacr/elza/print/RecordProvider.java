package cz.tacr.elza.print;

// TODO - JavaDoc - Lebeda

import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 27.6.16
 */
public interface RecordProvider {
     // TODO - JavaDoc - Lebeda
     public List<Record> getRecords();

     // TODO - JavaDoc - Lebeda
     public List<? extends RecordProvider> getRecordProviderChildern();
}
