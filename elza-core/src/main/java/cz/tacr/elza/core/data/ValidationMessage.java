package cz.tacr.elza.core.data;

import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.Part;
import cz.tacr.elza.drools.model.PartType;

/**
 * A translatable message of a validation: the qualified key ({@code <PACKAGE>/<KEY>}, or
 * {@code CORE/<KEY>} for a message of the core, {@link CoreMessage}), the message pattern as
 * written by the rule or the core (the fallback text) and the arguments.
 *
 * <p>Validation runs in background workers without a request, so the message is stored encoded
 * as one line of text in the error columns, next to the plain lines written by rules without a
 * key, and is rendered in the language of the reader when read
 * ({@link PackageTexts#render(String)}). The stored pattern keeps old rows readable after the
 * message changed or disappeared from its package.
 *
 * <p><b>Arguments.</b> A text and a number are stored as they are; a bare placeholder
 * ({@code {0}}) prints a number's digits verbatim (an identifier stays {@code 12383}), a styled
 * one ({@code {0,number,integer}}) formats it in the language of the reader. A {@link Ref}
 * names an item type, a specification, a part type, an entity class or another translated
 * entity and is rendered as its name in the language of the reader; a nested
 * {@link ValidationMessage} is rendered as its text. The objects of the rules and of the domain
 * convert to references by their class ({@link #of(String, String, Object...)}), so a rule passes
 * what it has: {@code $di}, {@code $recordType}, {@code part.getType()}.
 */
public final class ValidationMessage {

    /** Code of the "package" of messages built in the core, see {@link CoreMessage}. */
    public static final String CORE = "CORE";

    /** Nesting of messages in arguments is not rendered deeper than this. */
    private static final int MAX_DEPTH = 3;

    private static final String ENCODED_PREFIX = "{\"key\":";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_EMPTY);

    /**
     * Argument naming a translated entity: rendered as the name of the entity in the language of
     * the reader, as its code when the entity does not exist.
     *
     * @param type
     *            kind of the entity, the name of a {@link TranslationEntityType}
     * @param code
     *            code of the entity
     */
    public record Ref(@JsonProperty("t") String type, @JsonProperty("v") String code) {

        @JsonCreator
        public Ref {
            Objects.requireNonNull(type);
            code = code != null ? code : "";
        }

        public Ref(TranslationEntityType type, String code) {
            this(type.name(), code);
        }

        public TranslationEntityType entityType() {
            return TranslationEntityType.fromCode(type);
        }
    }

    @JsonProperty("key")
    private final String key;

    @JsonProperty("text")
    private final String text;

    @JsonProperty("args")
    private final List<Object> args;

    @JsonCreator
    private ValidationMessage(@JsonProperty("key") String key,
                              @JsonProperty("text") String text,
                              @JsonProperty("args") List<Object> args) {
        this.key = Objects.requireNonNull(key);
        this.text = text;
        List<Object> values = new ArrayList<>(args != null ? args.size() : 0);
        if (args != null) {
            for (Object arg : args) {
                values.add(decodeArg(arg));
            }
        }
        this.args = List.copyOf(values);
    }

    /**
     * @param key
     *            qualified key
     * @param text
     *            pattern in the source language ({@link java.text.MessageFormat}); null for a
     *            message whose source text is defined elsewhere
     * @param args
     *            arguments: numbers, booleans, references and nested messages are kept, the
     *            objects of the rules and of the domain become references (an item type, a
     *            specification, a part or its type, an entity class, a rule set, a structured
     *            type, a policy type; a description item names its item type), anything else is
     *            stored as text ({@code null} as an empty text)
     */
    public static ValidationMessage of(String key, String text, Object... args) {
        List<Object> values = new ArrayList<>(args != null ? args.length : 0);
        if (args != null) {
            for (Object arg : args) {
                values.add(convertArg(arg));
            }
        }
        return new ValidationMessage(key, text, values);
    }

    /**
     * Qualifies a key of a package: {@code KEY} becomes {@code <packageCode>/KEY}, a key already
     * qualified ({@code OTHER/KEY}, a message of another package) is kept.
     */
    public static String qualify(String packageCode, String key) {
        if (key.contains(TranslationEntityType.CODE_SEPARATOR) || StringUtils.isEmpty(packageCode)) {
            return key;
        }
        return packageCode + TranslationEntityType.CODE_SEPARATOR + key;
    }

    /** Argument rendered as the name of the item type. */
    public static Ref itemType(String code) {
        return new Ref(TranslationEntityType.ITEM_TYPE, code);
    }

    /** Argument rendered as the name of the item specification. */
    public static Ref itemSpec(String code) {
        return new Ref(TranslationEntityType.ITEM_SPEC, code);
    }

    /** Argument rendered as the name of the part type. */
    public static Ref partType(String code) {
        return new Ref(TranslationEntityType.PART_TYPE, code);
    }

    /** Argument rendered as the name of the entity class. */
    public static Ref apType(String code) {
        return new Ref(TranslationEntityType.AP_TYPE, code);
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    /**
     * @return arguments: {@link String}, {@link Number}, {@link Boolean}, {@link Ref} or a nested
     *         {@link ValidationMessage}
     */
    public List<Object> getArgs() {
        return args;
    }

    /**
     * @return the message as one line of text, see {@link #decode(String)}
     */
    public String encode() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The encoded message when it fits, else its {@link #sourceText()} cut to the length: for a
     * column that cannot hold the encoded form.
     */
    public String storable(int maxLength) {
        String encoded = encode();
        if (encoded.length() <= maxLength) {
            return encoded;
        }
        String source = sourceText();
        return source.length() <= maxLength ? source : source.substring(0, maxLength);
    }

    /**
     * The message in its source language with the arguments filled in, references as the codes
     * of the entities, nested messages as their source texts: the form for a full-text index or a
     * column that cannot hold the encoded message. The key when no text is known.
     */
    public String sourceText() {
        return sourceText(0);
    }

    private String sourceText(int depth) {
        if (text == null) {
            return key;
        }
        Object[] values = new Object[args.size()];
        for (int i = 0; i < values.length; i++) {
            Object arg = args.get(i);
            if (arg instanceof Ref ref) {
                values[i] = ref.code();
            } else if (arg instanceof ValidationMessage nested) {
                values[i] = depth < MAX_DEPTH ? nested.sourceText(depth + 1) : nested.getKey();
            } else {
                values[i] = arg;
            }
        }
        return format(text, Locale.ROOT, values);
    }

    /**
     * Text of a line of an error description for a full-text index: the {@link #sourceText()} of
     * an encoded message, any other line as it is.
     */
    public static String indexText(String line) {
        ValidationMessage message = decode(line);
        return message != null ? message.sourceText() : line;
    }

    /**
     * Fills a pattern with arguments. A number under a bare placeholder is printed as its digits;
     * a styled placeholder ({@code {0,number,integer}}, {@code {1,choice,...}}) formats it in the
     * locale.
     *
     * @return the text; the pattern itself when it is not a valid {@link MessageFormat} pattern
     *         (a text written by a rule is not checked at import)
     */
    public static String format(String pattern, Locale locale, Object... args) {
        try {
            MessageFormat messageFormat = new MessageFormat(pattern, locale);
            Format[] formats = messageFormat.getFormatsByArgumentIndex();
            Object[] values = args.clone();
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Number number && (i >= formats.length || formats[i] == null)) {
                    values[i] = number.toString();
                }
            }
            return messageFormat.format(values);
        } catch (IllegalArgumentException e) {
            return pattern;
        }
    }

    /**
     * @return true when the line is an encoded message
     */
    public static boolean isEncoded(String line) {
        return line != null && line.startsWith(ENCODED_PREFIX);
    }

    /**
     * @return the message encoded in the line, null for a plain text line or a damaged one
     */
    public static ValidationMessage decode(String line) {
        if (!isEncoded(line)) {
            return null;
        }
        try {
            return MAPPER.readValue(line, ValidationMessage.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Argument as stored: a reference for the objects of the rules and of the domain, text for
     * anything that is neither a number, a boolean, a reference nor a message.
     */
    private static Object convertArg(Object arg) {
        if (arg == null) {
            return "";
        }
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean
                || arg instanceof Ref || arg instanceof ValidationMessage) {
            return arg;
        }
        // rules
        if (arg instanceof ItemType itemType) {
            return itemType(itemType.getCode());
        }
        if (arg instanceof ItemSpec itemSpec) {
            return itemSpec(itemSpec.getCode());
        }
        if (arg instanceof DescItem descItem) {
            return itemType(descItem.getType());
        }
        if (arg instanceof PartType partType) {
            return partType(partType.value());
        }
        if (arg instanceof Part part) {
            return part.getType() != null ? partType(part.getType().value()) : "";
        }
        // domain
        if (arg instanceof cz.tacr.elza.core.data.ItemType itemType) {
            return itemType(itemType.getEntity().getCode());
        }
        if (arg instanceof RulItemType itemType) {
            return itemType(itemType.getCode());
        }
        if (arg instanceof RulItemSpec itemSpec) {
            return itemSpec(itemSpec.getCode());
        }
        if (arg instanceof RulPartType partType) {
            return partType(partType.getCode());
        }
        if (arg instanceof ApType apType) {
            return apType(apType.getCode());
        }
        if (arg instanceof RulRuleSet ruleSet) {
            return new Ref(TranslationEntityType.RULE_SET, ruleSet.getCode());
        }
        if (arg instanceof RuleSet ruleSet) {
            return new Ref(TranslationEntityType.RULE_SET, ruleSet.getCode());
        }
        if (arg instanceof RulStructuredType structuredType) {
            return new Ref(TranslationEntityType.STRUCTURED_TYPE, structuredType.getCode());
        }
        if (arg instanceof RulPolicyType policyType) {
            return new Ref(TranslationEntityType.POLICY_TYPE, policyType.getCode());
        }
        return arg.toString();
    }

    /**
     * Argument as read from JSON: an object with {@code t} is a reference, an object with
     * {@code key} a nested message.
     */
    private static Object decodeArg(Object arg) {
        if (arg instanceof Map<?, ?> map) {
            if (map.containsKey("key")) {
                return MAPPER.convertValue(map, ValidationMessage.class);
            }
            if (map.containsKey("t")) {
                Object code = map.get("v");
                return new Ref(String.valueOf(map.get("t")), code != null ? code.toString() : "");
            }
            return map.toString();
        }
        return arg != null ? arg : "";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValidationMessage other
                && key.equals(other.key) && Objects.equals(text, other.text) && args.equals(other.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, text, args);
    }

    @Override
    public String toString() {
        return encode();
    }
}
