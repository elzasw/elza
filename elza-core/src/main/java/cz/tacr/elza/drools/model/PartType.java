package cz.tacr.elza.drools.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;

/**
 * Part type of the Drools model: one instance per code, the seven CAM part types as constants.
 *
 * <p>Formerly an enum; now any part type a package declares has an instance, so an entity with a
 * part of a foreign type is built and validated like any other. Instances are interned, so
 * {@code ==} and {@code equals} agree and rules compare against the constants as before:
 * {@code Part(type == PartType.PT_NAME)}. A rule of a package with its own part type compares the
 * code: {@code Part(typeCode == "PT_NOTE")}, or {@code type == PartType.of("PT_NOTE")}.
 */
public final class PartType {

    private static final Map<String, PartType> INSTANCES = new ConcurrentHashMap<>();

    public static final PartType PT_BODY = of("PT_BODY");
    public static final PartType PT_CRE = of("PT_CRE");
    public static final PartType PT_EVENT = of("PT_EVENT");
    public static final PartType PT_EXT = of("PT_EXT");
    public static final PartType PT_IDENT = of("PT_IDENT");
    public static final PartType PT_NAME = of("PT_NAME");
    public static final PartType PT_REL = of("PT_REL");

    private final String code;

    private PartType(final String code) {
        this.code = code;
    }

    /** The part type of the code; created when seen for the first time. */
    public static PartType of(final String code) {
        Validate.notBlank(code, "Part type code is required");
        return INSTANCES.computeIfAbsent(code, PartType::new);
    }

    /** Same as {@link #of(String)}; the name the CAM rules know. */
    public static PartType fromValue(final String v) {
        return of(v);
    }

    /** Same as {@link #of(String)}; the name of the former enum. */
    public static PartType valueOf(final String v) {
        return of(v);
    }

    public String getCode() {
        return code;
    }

    /** The code; the name of the former enum. */
    public String value() {
        return code;
    }

    /** The code; the name of the former enum. */
    public String name() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
