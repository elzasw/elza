package cz.tacr.elza.dao.api.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.XmlUtils;
import cz.tacr.elza.dao.bo.DaoFileBo;
import cz.tacr.elza.dao.bo.resource.DaoFileInfo;
import cz.tacr.elza.dao.service.DcsResourceService;
import cz.tacr.elza.ws.types.v1.DaoImport;

@RestController
@RequestMapping(value = "/dl")
public class DcsResourceController {

	@Autowired
	private DcsResourceService resourceService;

	@RequestMapping(value = "/import/{packageIdentifiers}", method = RequestMethod.GET)
	public void downloadDaoImport(
			@PathVariable String[] packageIdentifiers,
			HttpServletResponse response) throws IOException {
		DaoImport daoImport = resourceService.getDaoImport(Arrays.asList(packageIdentifiers));

		response.setContentType(MediaType.APPLICATION_XML_VALUE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"DaoImport.xml\"");
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		try (OutputStream os = response.getOutputStream()) {
			XmlUtils.marshalXmlType(DaoImport.class, daoImport, os);
		}
	}

	@RequestMapping(value = "/file/{packageIdentifier}/{daoIdentifier}/{fileIdentifier:.+}", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadDaoFile(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			@PathVariable String fileIdentifier) throws IOException {
		DaoFileBo daoFile = resourceService.getDaoFile(packageIdentifier, daoIdentifier, fileIdentifier);
		DaoFileInfo fileInfo = daoFile.getFileInfo();

		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (fileInfo.getMimeType() != null) {
			mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
		}
		return ResponseEntity
				.ok()
				.contentLength(fileInfo.getSize())
				.contentType(mediaType)
				.body(new PathResource(fileInfo.getFilePath()));
	}

	@RequestMapping(value = "/list/{packageIdentifier}/{daoIdentifier}", method = RequestMethod.GET)
	public String listDaoFiles(
			@PathVariable String packageIdentifier,
			@PathVariable String daoIdentifier,
			Model model)
			throws IOException {
		Collection<DaoFileBo> daoFiles = resourceService.getDaoFiles(packageIdentifier, daoIdentifier);

		StringBuilder sb = new StringBuilder("<html>\n<head><title>Dao files</title></head>\n");
		sb.append("<body><div style='white-space:nowrap'>\nrepositoryIdentifier: ");
		sb.append(DCStorageConfig.get().getRepositoryIdentifier());
		sb.append("<br/>\npackageIdentifier: ");
		sb.append(packageIdentifier);
		sb.append("<br/>\ndaoIdentifier: ");
		sb.append(daoIdentifier);
		sb.append("<br/>\n");
		for (DaoFileBo daoFile : daoFiles) {
			sb.append("<br/># <a href='/");
			sb.append(DCStorageConfig.get().getRepositoryIdentifier());
			sb.append("/dl/file/");
			sb.append(packageIdentifier);
			sb.append('/');
			sb.append(daoIdentifier);
			sb.append('/');
			sb.append(daoFile.getIdentifier());
			sb.append("' target='_blank'>");
			sb.append(daoFile.getIdentifier());
			sb.append("</a>\n");
		}
		sb.append("</div><body>\n</html>");
		return sb.toString();
	}
}