package cz.tacr.elza.controller;

import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.File;
import java.net.URL;
import java.util.function.Function;

import static com.jayway.restassured.RestAssured.given;


/**
 * @author Petr Compel
 * @since 17.2.2016
 */
public class RuleControllerTest extends AbstractControllerTest {

    private static final RestAssuredConfig UTF8_ENCODER_CONFIG = RestAssuredConfig.newConfig().encoderConfig(
            EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

    private static final Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);
    protected static final String CONTENT_TYPE_HEADER = "content-type";
    private static final Header MULTIPART_HEADER = new Header(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA_VALUE);

    private static final String IMPORT_PACKAGE = RULE_CONTROLLER_URL + "/importPackage";
    private static final String DELETE_PACKAGE = RULE_CONTROLLER_URL + "/deletePackage/";
    private static final String EXPORT_PACKAGE = RULE_CONTROLLER_URL + "/exportPackage/";

    @Test
    public void getDataTypesTest() {
        getDataTypes();
    }

    @Test
    public void getDescItemTypesTest() {
        getDescItemTypes();
    }

    @Test
    public void getPackagesTest() {
        getPackages();
    }

    @Test
    public void getRuleSetsTest() {
        getRuleSets();
    }

    @Test
    public void deleteImportExportPackageTest() {
        deletePackage(getPackages().get(0).getCode());
        importPackage();
        exportPackage(getPackages().get(0).getCode());
    }

    private void importPackage() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(PACKAGE_FILE);
        File file = new File(url.getPath());
        multipart(spec -> spec.multiPart("file", file), IMPORT_PACKAGE);
    }

    private void deletePackage(final String code) {
        get(DELETE_PACKAGE + code);
    }

    private void exportPackage(final String code) {
        get(EXPORT_PACKAGE + code);
    }


    public static Response multipart(Function<RequestSpecification, RequestSpecification> params,
                                     String url) {
        Assert.assertNotNull(params);
        Assert.assertNotNull(url);

        RequestSpecification requestSpecification = params.apply(given());

        requestSpecification.header(MULTIPART_HEADER).log().all().config(UTF8_ENCODER_CONFIG);

        Response response = requestSpecification.post(url);
        logger.info("Response status: " + response.statusLine() + ", response body:");
        response.prettyPrint();
        Assert.assertEquals(HttpStatus.OK.value(), response.statusCode());

        return response;
    }
}
