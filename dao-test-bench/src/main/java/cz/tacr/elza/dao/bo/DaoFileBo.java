package cz.tacr.elza.dao.bo;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import cz.tacr.elza.dao.bo.resource.DaoConfig;
import cz.tacr.elza.dao.bo.resource.DaoFileInfo;
import cz.tacr.elza.dao.bo.resource.DaoFileInfoResource;
import cz.tacr.elza.dao.common.XmlUtils;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.types.v1.File;

public class DaoFileBo {

	private static final Logger LOG = LoggerFactory.getLogger(DaoFileBo.class);

	private final DaoBo dao;

	private final String identifier;

	private DaoFileInfo fileInfo;

	public DaoFileBo(DaoBo dao, String identifier, boolean init) {
		Assert.notNull(dao);
		Assert.notNull(identifier);
		this.dao = dao;
		this.identifier = identifier;
		if (init) {
			initFileInfoResource();
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public DaoFileInfo getFileInfo() {
		if (fileInfo == null) {
			initFileInfoResource();
		}
		return fileInfo;
	}

	public File export() {
		File file = new File();
		file.setIdentifier(identifier);
		DaoFileInfo info = getFileInfo();
		file.setMimetype(info.getMimeType());
		file.setCreated(XmlUtils.convertDate(info.getCreated()));
		file.setSize(info.getSize());
		return file;
	}

	@Override
	public String toString() {
		return "DaoFileBo [packageIdentifier=" + dao.getDaoPackage().getIdentifier() + ", daoIdentifier="
				+ dao.getIdentifier() + ", identifier=" + identifier + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(dao, identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof DaoFileBo) {
			DaoFileBo o = (DaoFileBo) obj;
			return dao.equals(o.dao) && identifier.equals(o.identifier);
		}
		return false;
	}

	private void initFileInfoResource() {
		DaoFileInfoResource fileInfoResource = new DaoFileInfoResource(
				dao.getDaoPackage().getIdentifier(), dao.getIdentifier(), identifier);
		try {
			fileInfoResource.init();
		} catch (Exception e) {
			throw new DaoComponentException("cannot read dao file attributes", e);
		}
		fileInfo = fileInfoResource.getResource();
		for (Map<String, Object> map : dao.getConfig().getFileAttributes()) {
			if (identifier.equals(map.get(DaoConfig.FILE_IDENTIFIER_ATTR_NAME))) {
				setFileAttribute(map, DaoConfig.FILE_MIME_TYPE_ATTR_NAME, String.class, fileInfo::setMimeType);
				setFileAttribute(map, DaoConfig.FILE_CREATED_ATTR_NAME, Date.class, fileInfo::setCreated);
				setFileAttribute(map, DaoConfig.FILE_SIZE_ATTR_NAME, Long.class, fileInfo::setSize);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> void setFileAttribute(Map<String, Object> attributes, String name, Class<T> type, Consumer<T> setter) {
		Object value = attributes.get(name);
		if (type.isInstance(value)) {
			setter.accept((T) value);
		} else if (value != null) {
			LOG.error("mismatched file attribute type, name:" + name + ", expected:" + type.getName() + ", present:"
					+ value.getClass().getName());
		}
	}
}