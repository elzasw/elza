package cz.tacr.elza.daoimport.service.vo;

import java.util.ArrayList;
import java.util.List;

public class ImportBundle {
	final String bundleName;
	
	final List<DaoFile> daoFiles = new ArrayList<>();
	
	ImportBundle(final String bundleName) {
		this.bundleName = bundleName;
	}
	
	public String getBundleName() {
		return bundleName;
	}

	public void add(DaoFile file) {
		daoFiles.add(file);		
	}
	
	public List<DaoFile> getFiles() {
		return daoFiles;
	}
}
