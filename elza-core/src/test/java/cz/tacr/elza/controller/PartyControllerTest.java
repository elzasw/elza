package cz.tacr.elza.controller;

import com.jayway.restassured.response.Response;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


/**
 *
 */
public class PartyControllerTest extends AbstractControllerTest {

    @Test
    public void getInstitutionsTest() {
        Response response = get(INSTITUTIONS);
        List<ParInstitutionVO> parInstitutionVOS = Arrays.asList(response.getBody().as(ParInstitutionVO[].class));
        response = get(spec -> spec.param("hasFund", true), INSTITUTIONS);
        parInstitutionVOS = Arrays.asList(response.getBody().as(ParInstitutionVO[].class));
    }
}
