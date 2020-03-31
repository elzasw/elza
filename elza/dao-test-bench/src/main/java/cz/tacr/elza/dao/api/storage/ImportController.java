package cz.tacr.elza.dao.api.storage;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.common.CoreServiceProvider;
import cz.tacr.elza.dao.common.XmlUtils;
import cz.tacr.elza.dao.service.ResourceService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.types.v1.DaoImport;

@RestController
@RequestMapping(value = "/import")
public class ImportController {

	@Autowired
	private ResourceService resourceService;

	/**
	 * Downloads XML serialized DaoImport. Expecting list of package identifiers.
	 */
	@RequestMapping(value = "/{packageIdentifiers}", method = RequestMethod.GET)
	public void downloadDaoImport(@PathVariable String[] packageIdentifiers, HttpServletResponse response)
			throws IOException {
		DaoImport daoImport = resourceService.getDaoImport(packageIdentifiers);

		response.setContentType(MediaType.APPLICATION_XML_VALUE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dao-import.xml\"");
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
		try (OutputStream os = response.getOutputStream()) {
			XmlUtils.marshalXmlType(DaoImport.class, daoImport, os);
		}
	}

	/**
	 * Imports DaoImport to external system (ELZA). Expecting list of package identifiers and system identifier.
	 * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{packageIdentifiers}/system/{systemIdentifier}", method = RequestMethod.POST)
    public void importPckgs(@PathVariable String systemIdentifier, @PathVariable String[] packageIdentifiers)
			throws CoreServiceException {
		DaoImport daoImport = resourceService.getDaoImport(packageIdentifiers);
		DaoService service = CoreServiceProvider.getDaoCoreService(systemIdentifier);
		service._import(daoImport);
	}
}