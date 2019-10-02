package cz.tacr.elza.controller;

import cz.tacr.elza.controller.vo.SingleSignOnEntityVO;
import cz.tacr.elza.security.saml.SamlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired(required = false)
    private MetadataManager samlMetadata;

    @Autowired(required = false)
    private SamlProperties samlProperties;

    @RequestMapping(value = "/sso", method = RequestMethod.GET)
    public List<SingleSignOnEntityVO> getSSOEntities() {
        List<SingleSignOnEntityVO> result = new ArrayList<>();
        if (samlMetadata != null && samlProperties != null) {
            for (String entityName : samlMetadata.getIDPEntityNames()) {
                String title;
                if (samlProperties.getIdpUrl().equalsIgnoreCase(entityName)) {
                    title = samlProperties.getTitle();
                } else {
                    title = entityName;
                }
                result.add(new SingleSignOnEntityVO(title, "/saml/login?idp=" + entityName));
            }
        }
        return result;
    }

}
