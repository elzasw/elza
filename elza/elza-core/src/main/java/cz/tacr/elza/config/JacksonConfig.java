package cz.tacr.elza.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link JsonNullableModule} on the application's Spring-managed
 * {@code ObjectMapper}. Spring Boot installs every {@code com.fasterxml.jackson}
 * {@code Module} bean on its auto-configured mapper, so declaring this bean makes
 * the whole application handle {@code JsonNullable<>} — the type the OpenAPI
 * generator uses for a schema's nullable/optional fields.
 *
 * <p>Without the module a <em>set</em> {@code JsonNullable} value serializes as
 * {@code {"present":true}}, silently dropping the wrapped payload. This bit the
 * AI provider client VOs (e.g. {@code ToolResult.result}, a
 * {@code JsonNullable<Object>}): the generated {@code ApiClient} registers the
 * module for its own HTTP calls, but when Elza re-serializes those VOs through
 * the shared {@code ObjectMapper} — storing the tool-result transparency events
 * read back by {@code AiActivityMapper} — the search result (its {@code funds}
 * and {@code totalCount}) was lost, so the activity feed showed "0 results" for a
 * search that had found some. Registering the module once, globally, fixes every
 * such (de)serialization rather than per call site.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
