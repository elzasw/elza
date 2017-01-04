package cz.tacr.elza.dao.bo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import cz.tacr.elza.dao.PathResolver;
import cz.tacr.elza.dao.descriptor.DaoConfig;
import cz.tacr.elza.dao.descriptor.DaoFileInfo;
import cz.tacr.elza.dao.exception.DaoComponentException;

public class DaoFileBo implements DescriptorTarget<DaoFileInfo> {

	private final DaoBo daoBo;

	private final String identifier;

	private Map<String, Object> attributes;

	public DaoFileBo(DaoBo daoBo, String identifier) {
		Assert.notNull(daoBo);
		Assert.notNull(identifier);
		this.daoBo = daoBo;
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Map<String, Object> getAttributes() {
		if (attributes == null) {
			attributes = daoBo.getDescriptor().getFileAttributes().stream()
				.filter(m -> identifier.equals(m.get(DaoConfig.FILE_IDENTIFIER_ATTR_NAME)))
				.findFirst().orElse(new HashMap<>());
		}
		return attributes;
	}

	public String getMimeType() {
		Object value = getAttributes().get(DaoConfig.FILE_MIME_TYPE_ATTR_NAME);
		if (value instanceof String) {
			return (String) value;
		}
		return getDescriptor().getMimeType();
	}

	public Date getCreated() {
		Object value = getAttributes().get(DaoConfig.FILE_CREATED_ATTR_NAME);
		if (value instanceof Date) {
			return (Date) value;
		}
		return getDescriptor().getCreated();
	}

	public Long getSize() {
		Object value = getAttributes().get(DaoConfig.FILE_SIZE_ATTR_NAME);
		if (value instanceof Long) {
			return (Long) value;
		}
		return getDescriptor().getSize();
	}

	@Override
	public String toString() {
		return "DaoFileBo [packageIdentifier:" + daoBo.getDaoPackage().getIdentifier()
				+ ", daoIdentifier:" + daoBo.getIdentifier() + ", identifier:" + identifier + "]";
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
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
			return daoBo.equals(o.daoBo) && identifier.equals(o.identifier);
		}
		return false;
	}

	@Override
	public DaoFileInfo getDescriptor() {
		Path path = PathResolver.resolveDaoFilePath(daoBo.getDaoPackage().getIdentifier(), daoBo.getIdentifier(), identifier);
		DaoFileInfo info = new DaoFileInfo();
		try {
			info.setMimeType(Files.probeContentType(path));
			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
			info.setCreated(new Date(attrs.creationTime().toMillis()));
			info.setSize(attrs.size());
		} catch (IOException e) {
			throw new DaoComponentException("cannot create dao file descriptor", e);
		}
		return info;
	}

	@Override
	public void saveDescriptor() {
		throw new UnsupportedOperationException();
	}
}
