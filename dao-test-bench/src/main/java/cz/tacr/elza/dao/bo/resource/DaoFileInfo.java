package cz.tacr.elza.dao.bo.resource;

import java.nio.file.Path;
import java.util.Date;

public class DaoFileInfo {

	private Path filePath;

	private String mimeType;

	private Date created;

	private Long size;

	public Path getFilePath() {
		return filePath;
	}

	public void setFilePath(Path filePath) {
		this.filePath = filePath;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}
}