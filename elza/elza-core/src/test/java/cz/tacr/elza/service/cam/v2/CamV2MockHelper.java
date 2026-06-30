package cz.tacr.elza.service.cam.v2;

import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import cz.tacr.cam.v2.client.controller.vo.RequestProcessState;
import cz.tacr.cam.v2.schema.cam.BatchChangeFailureXml;
import cz.tacr.cam.v2.schema.cam.BatchChangeSuccessXml;
import cz.tacr.cam.v2.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.DateTimeXml;
import cz.tacr.cam.v2.schema.cam.EntitiesXml;
import cz.tacr.cam.v2.schema.cam.EntityIdXml;
import cz.tacr.cam.v2.schema.cam.EntityIssuesXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.IssueSeverityXml;
import cz.tacr.cam.v2.schema.cam.PartRefXml;
import cz.tacr.cam.v2.schema.cam.PartTypeXml;
import cz.tacr.cam.v2.schema.cam.StringXml;
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

    /**
     * Stub {@code GET /batches/{id}/result} with a {@code BatchChangeFailure} response
     * built from the given issues. When {@code forceKey} is non-null, it's attached to
     * the failure so the client (and the user) can retry with force.
     */
    public StubMapping stubBatchResultFailure(UUID batchId, List<IssueSpec> issues, String forceKey) {
        BatchChangeFailureXml failure = new BatchChangeFailureXml();
        EntityIssuesXml entityIssues = new EntityIssuesXml();
        for (IssueSpec spec : issues) {
            entityIssues.getIssue().add(spec.toXml());
        }
        failure.getIssues().add(entityIssues);
        if (forceKey != null) {
            failure.setForceKey(new StringXml(forceKey));
        }
        return stubResult(batchId, marshal(BatchChangeFailureXml.class, failure));
    }

    /**
     * Minimal description of an issue used by {@link #stubBatchResultFailure}.
     * Only severity and message are required; everything else is optional.
     */
    public static final class IssueSpec {
        private final IssueSeverityXml severity;
        private final String message;
        private final String ruleCode;
        private final String detail;
        private String partUuid;
        private PartTypeXml partType;

        public IssueSpec(IssueSeverityXml severity, String message, String ruleCode, String detail) {
            this.severity = severity;
            this.message = message;
            this.ruleCode = ruleCode;
            this.detail = detail;
        }

        public static IssueSpec warning(String message, String ruleCode) {
            return new IssueSpec(IssueSeverityXml.WARNING, message, ruleCode, null);
        }

        public static IssueSpec error(String message, String ruleCode) {
            return new IssueSpec(IssueSeverityXml.ERROR, message, ruleCode, null);
        }

        /** Attach a partRef to this issue so the resolver can map it back to an ELZA partId. */
        public IssueSpec withPart(String partUuid, PartTypeXml partType) {
            this.partUuid = partUuid;
            this.partType = partType;
            return this;
        }

        ExistingIssueXml toXml() {
            ExistingIssueXml xml = new ExistingIssueXml();
            xml.setUuid(new UuidXml(UUID.randomUUID().toString()));
            xml.setSeverity(severity);
            xml.setMessage(new StringXml(message));
            if (ruleCode != null) {
                xml.setRuleCode(new CodeXml(ruleCode));
            }
            if (detail != null) {
                xml.setDetail(new StringXml(detail));
            }
            if (partUuid != null) {
                xml.setPartRef(new PartRefXml(new UuidXml(partUuid), partType));
            }
            // `from` is required by the schema — any point in time works for tests
            xml.setFrom(new DateTimeXml(OffsetDateTime.now()));
            return xml;
        }
    }

    /**
     * Stub {@code GET /entities/{entityId}}; used by the import flow when the
     * download processor has exactly one queue item to fetch.
     */
    public StubMapping stubGetEntityById(long entityId, EntityXml entityXml) {
        StubMapping stub = wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(API_V2 + "/entities/" + entityId))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/xml;charset=UTF-8")
                                .withBody(marshal(EntityXml.class, entityXml))));
        stubs.add(stub);
        return stub;
    }

    /**
     * Stub {@code POST /export/snapshots}; used by the import flow when the
     * download processor batches multiple queue items into a single call.
     */
    public StubMapping stubExportSnapshots(EntitiesXml entitiesXml) {
        StubMapping stub = wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo(API_V2 + "/export/snapshots"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "text/xml;charset=UTF-8")
                                .withBody(marshal(EntitiesXml.class, entitiesXml))));
        stubs.add(stub);
        return stub;
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
