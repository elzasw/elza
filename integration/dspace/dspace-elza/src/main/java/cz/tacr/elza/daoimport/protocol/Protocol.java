package cz.tacr.elza.daoimport.protocol;

import java.io.Closeable;

public interface Protocol extends Closeable {

	void add(String message);

}
