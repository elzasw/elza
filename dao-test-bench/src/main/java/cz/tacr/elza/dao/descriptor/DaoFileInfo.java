package cz.tacr.elza.dao.descriptor;

import java.util.Date;

public class DaoFileInfo implements Descriptor {

	private String mimeType;

	private Date created;

	private Long size;

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

	@Override
	public boolean isDirty() {
		return true;
	}
}
