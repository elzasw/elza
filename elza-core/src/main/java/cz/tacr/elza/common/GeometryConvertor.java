package cz.tacr.elza.common;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Geometry converter for data-exchange.
 */
public class GeometryConvertor {

    private GeometryConvertor() {
    }

    public static String convert(Geometry value) {
        if (value == null) {
            return null;
        }
        WKTWriter writer = new WKTWriter();
        writer.setFormatted(false);
        return writer.write(value);
    }

    public static Geometry convert(String value) {
        try {
            WKTReader reader = new WKTReader();
            Geometry result = reader.read(value);
            return result;
        } catch (ParseException e) {
            throw new SystemException("Failed to parse geometry value", e, BaseCode.PROPERTY_IS_INVALID)
                    .set("value", value);
        }
    }

    public static class GeometryJsonSerializer extends JsonSerializer<Geometry> {

        @Override
        public void serialize(Geometry value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
            String str = convert(value);
            gen.writeString(str);
        }
    }

    public static class GeometryJsonDeserializer extends JsonDeserializer<Geometry> {

        @Override
        public Geometry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            // read value as string
            JsonNode node = p.getCodec().readTree(p);
            Validate.isTrue(node.isValueNode());
            String value = node.asText();
            // convert to geometry
            Geometry geo = convert(value);
            return geo;
        }
    }
}
