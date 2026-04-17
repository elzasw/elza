package cz.tacr.elza.service.cam.v2;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import cz.tacr.cam.v2.client.controller.vo.RequestProcessState;
import cz.tacr.cam.v2.schema.cam.BatchChangeSuccessXml;
import cz.tacr.cam.v2.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.v2.schema.cam.EntityIdXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

/**
 * Configures WireMock stubs for the CAM v2 REST endpoints used by the
 * export flow ({@code POST /batches}, {@code GET /batches/{id}/status},
 * {@code GET /batches/{id}/result}).
 *
 * Paths start with {@code /api/v2} because
 * {@link cz.tacr.elza.cam.v2.CamInstance} appends that prefix to the
 * {@code ApExternalSystem.url}. The POST stub uses {@code urlPathEqualTo}
 * because the generated client appends {@code ?force=...} query params.
 *
 * Stubs registered through this helper are tracked so {@link #cleanUp()}
 * can remove only what this helper added.
 */
public class CamV2MockHelper {

    private static final String API_V2 = "/api/v2";

    private final WireMockServer wireMockServer;
    private final List<StubMapping> stubs = new ArrayList<>();

    public CamV2MockHelper(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Stub {@code POST /batches}; returns the batch UUID.
     *
     * The generated client's deserializer only handles JSON or raw String for
     * the {@code UUID} return type — responding with {@code application/json}
     * and a JSON-quoted UUID is what the real CAM server does too.
     */
    public StubMapping stubPostBatch(UUID responseBatchId) {
        StubMapping stub = wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(API_V2 + "/batches"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json;charset=UTF-8")
                                .withBody("\"" + responseBatchId + "\"")));
        stubs.add(stub);
        return stub;
    }

    /** Stub {@code GET /batches/{id}/status}. */
    public StubMapping stubBatchStatus(UUID batchId, RequestProcessState state) {
        String body = "{\"state\":\"" + state.getValue() + "\"}";
        StubMapping stub = wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(API_V2 + "/batches/" + batchId + "/status"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json;charset=UTF-8")
                                .withBody(body)));
        stubs.add(stub);
        return stub;
    }

    /**
     * Stub {@code GET /batches/{id}/result} with a {@code BatchChangeSuccess} response.
     * Mirrors CAM accepting and storing the batch.
     */
    public StubMapping stubBatchResultSuccess(UUID batchId,
                                              long entityId,
                                              String entityUuid,
                                              String localId,
                                              String revisionUuid) {
        BatchChangeSuccessXml success = new BatchChangeSuccessXml();
        BatchEntityRecordRevXml rev = new BatchEntityRecordRevXml();
        rev.setEntityId(new EntityIdXml(entityId));
        rev.setEntityUuid(new UuidXml(entityUuid));
        rev.setLocalId(localId);
        rev.setRev(new UuidXml(revisionUuid));
        success.getRevision().add(rev);
        return stubResult(batchId, marshal(BatchChangeSuccessXml.class, success));
    }

    private StubMapping stubResult(UUID batchId, String body) {
        StubMapping stub = wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(API_V2 + "/batches/" + batchId + "/result"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/xml;charset=UTF-8")
                                .withBody(body)));
        stubs.add(stub);
        return stub;
    }

    public void cleanUp() {
        for (StubMapping stub : stubs) {
            wireMockServer.removeStub(stub);
        }
        stubs.clear();
    }

    private static <T> String marshal(Class<T> clazz, T value) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(clazz);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(value, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to marshal " + clazz.getSimpleName(), e);
        }
    }
}
