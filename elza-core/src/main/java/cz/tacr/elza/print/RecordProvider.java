package cz.tacr.elza.print;


import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 27.6.16
 */
public interface RecordProvider {

     /**
      * @return seznam všech recordů vázaných přímo na objektu
      */
     public List<Record> getRecords();

     /**
      * @return seznam podřízených objektů obsahujících další recordy
      */
     public List<? extends RecordProvider> getRecordProviderChildern();
}
