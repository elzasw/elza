package cz.tacr.elza.dao.bo.resource;

import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.common.XmlUtils;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DigitizationRequestResource extends AbstractStorageResource<DigitizationRequest> {

	private final Path resourcePath;

	private DigitizationRequestResource(Path resourcePath) {
		this.resourcePath = resourcePath;
	}

	public DigitizationRequestResource(String requestIdentifier) {
		this(PathResolver.resolveDigitizationRequestInfoPath(requestIdentifier));
	}

	public String getIdentifier() {
		return resourcePath.getParent().getFileName().toString();
	}

	public void delete() throws IOException {
		Files.delete(resourcePath);
		Files.delete(resourcePath.getParent());
		clearCached();
	}

	@Override
	protected DigitizationRequest loadResource() throws Exception {
		try (InputStream is = Files.newInputStream(resourcePath)) {
			return XmlUtils.unmarshalXmlType(DigitizationRequest.class, is);
		}
	}

	public static DigitizationRequestResource create(DigitizationRequest digitizationRequest)
			throws IOException {
		Path filePath = PathResolver.createDigitizationRequestInfoPath(10);
		Path dirPath = filePath.getParent();
		Files.createDirectories(dirPath);
		try (OutputStream os = Files.newOutputStream(filePath)) {
			XmlUtils.marshalXmlType(DigitizationRequest.class, digitizationRequest, os);
		}
		return new DigitizationRequestResource(filePath);
	}
}
