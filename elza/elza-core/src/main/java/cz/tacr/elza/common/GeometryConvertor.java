package cz.tacr.elza.common;

import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.geotools.geometry.jts.WKBReader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import cz.tacr.elza.exception.BusinessException;
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

    public static byte[] convertToWkb(Geometry value) {
        if (value == null) {
            return null;
        }
        WKBWriter writer = new WKBWriter();
        return writer.write(value);
    }

    public static Geometry convertWkb(byte[] value) {
        try {
            Geometry geom = new WKBReader().read(value);
            return geom;
        } catch (ParseException e) {
            throw new SystemException("Failed to parse geometry value", e, BaseCode.PROPERTY_IS_INVALID);
        }
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
