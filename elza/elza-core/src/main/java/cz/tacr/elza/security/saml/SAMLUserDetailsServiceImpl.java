/*
 * Copyright 2019 Vincenzo De Notaris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.tacr.elza.security.saml;

// import org.springframework.security.saml.SAMLCredential;
// import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;


@Service
public class SAMLUserDetailsServiceImpl /*implements SAMLUserDetailsService*/
{

    /*
    private static final Logger logger = LoggerFactory.getLogger(SAMLUserDetailsServiceImpl.class);
    
    @Autowired
    private UserService userService;
    
    @Transactional
    public Object loadUserBySAML(SAMLCredential credential)
    		throws UsernameNotFoundException {
    	String userID = credential.getNameID().getValue();
    
    	List<UsrAuthentication> authentications = userService.findAuthentication(userID, UsrAuthentication.AuthType.SAML2);
    
    	if (authentications.size() == 1) {
    		UsrAuthentication authentication = authentications.get(0);
    		logger.info(userID + " is logged in");
    		return userService.createUserDetail(authentication.getUser());
    	} else {
    		if (authentications.size() > 0) {
    			throw new SystemException("Pro " + userID + " existuje více záznamů", BaseCode.INVALID_STATE);
    		}
    		throw new SystemException("Nepovolené přihlášení pro " + userID);
    	}
    }
    */
}
