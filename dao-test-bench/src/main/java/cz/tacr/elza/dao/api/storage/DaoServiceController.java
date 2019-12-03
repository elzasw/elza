package cz.tacr.elza.dao.api.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.common.CoreServiceProvider;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoService;

@RestController
@RequestMapping(value = "/daoservice")
public class DaoServiceController {

    /**
     * Remove package from external system (ELZA).
     * 
     * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/remove/{daoPackageId}/system/{systemIdentifier}", method = { RequestMethod.GET,
            RequestMethod.POST, RequestMethod.DELETE })
    public void removePackage(@PathVariable String systemIdentifier, @PathVariable String daoPackageId)
            throws CoreServiceException {
        DaoService service = CoreServiceProvider.getDaoCoreService(systemIdentifier);
        service.removePackage(daoPackageId);
    }
}
