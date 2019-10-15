package cz.tacr.elza.dao.bo.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.common.XmlUtils;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;

public class DigitizationRequestInfoResource extends AbstractStorageResource<DigitizationRequestInfo> {

	private final Path resourcePath;

	private DigitizationRequestInfoResource(Path resourcePath) {
		this.resourcePath = resourcePath;
	}

	public DigitizationRequestInfoResource(String requestIdentifier) {
		this(PathResolver.resolveDigitizationRequestInfoPath(requestIdentifier));
	}

	public String getIdentifier() {
		return PathResolver.getDigitizationRequestInfoName(resourcePath);
	}

	public void save() throws IOException {
		if (!isInitialized()) {
			throw new IllegalStateException("resource not initialized");
		}
		try (OutputStream os = Files.newOutputStream(resourcePath)) {
			XmlUtils.marshalXmlRoot(DigitizationRequestInfo.class, get(), os);
		}
	}

	@Override
	protected DigitizationRequestInfo loadResource() throws Exception {
		try (InputStream is = Files.newInputStream(resourcePath)) {
			return XmlUtils.unmarshalXmlRoot(DigitizationRequestInfo.class, is);
		}
	}

	public static DigitizationRequestInfoResource create(DigitizationRequest request)
			throws IOException {
		Path filePath = PathResolver.createDigitizationRequestInfoPath(10);
		Path dirPath = filePath.getParent();
		Files.createDirectories(dirPath);
		try (OutputStream os = Files.newOutputStream(filePath)) {
			XmlUtils.marshalXmlRoot(DigitizationRequestInfo.class, wrapRequest(request), os);
		}
		return new DigitizationRequestInfoResource(filePath);
	}
	
	private static JAXBElement<DigitizationRequest> wrapRequest(DigitizationRequest request) {
		return XmlUtils.wrapElement(DigitizationRequestInfo.NAME, DigitizationRequest.class, request);
	}
}
