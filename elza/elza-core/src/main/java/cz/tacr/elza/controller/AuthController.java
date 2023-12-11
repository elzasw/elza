package cz.tacr.elza.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /*
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
    */

}
