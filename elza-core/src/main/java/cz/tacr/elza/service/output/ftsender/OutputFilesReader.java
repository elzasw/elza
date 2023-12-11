package cz.tacr.elza.service.output.ftsender;

import java.util.Iterator;
import java.util.List;

// import com.lightcomp.ft.core.send.items.SimpleFile;
// import com.lightcomp.ft.core.send.items.SourceItem;
// import com.lightcomp.ft.core.send.items.SourceItemReader;

import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.service.DmsService;

public class OutputFilesReader
//implements SourceItemReader 
{

	final private List<ArrOutputFile> files;
	private Iterator<ArrOutputFile> iter;
	
	final private DmsService dmsService;

	public OutputFilesReader(final List<ArrOutputFile> files, 
							 final DmsService dmsService) {
		this.files = files;
		this.dmsService = dmsService;
	}

    //	@Override
    //	public void open() {
    //		iter = files.iterator();
    //	}
    //
    //	@Override
    //	public boolean hasNext() {
    //		return iter.hasNext();
    //	}
    //
    //	@Override
    //	public SourceItem getNext() {
    //		ArrOutputFile outputFile = iter.next();
    //		
    //		Path filePath = dmsService.getFilePath(outputFile);
    //		
    //		return new SimpleFile(filePath, outputFile.getFileName());
    //	}
    //
    //	@Override
    //	public void close() {
    //		iter = null;
    //	}
    //
}
