package cz.tacr.elza.repository.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.vo.AipFieldName;

/**
 * Binds the filter contract to the entity model.
 *
 * Neither side can check this alone: the contract does not know where a field lives, and the
 * mapping does not know which fields the contract offers. Adding a field to main.tsp without
 * mapping it, or mapping one onto an attribute of the wrong type, fails here.
 */
public class AipFieldMappingTest {

    @Test
    public void everyContractFieldIsMapped() {
        for (AipFieldName fieldName : AipFieldName.values()) {
            assertNotNull(AipFieldMapping.of(fieldName), "no mapping for field " + fieldName.getValue());
        }
        assertEquals(AipFieldName.values().length, AipFieldMapping.values().length,
                "the mapping declares fields the contract does not offer");
    }

    /**
     * The value type a field declares must match the Java type of the attribute behind it -
     * otherwise the parameter would be bound in a type the column cannot be compared with.
     */
    @Test
    public void declaredValueTypeMatchesTheEntityAttribute() {
        for (AipFieldMapping mapping : AipFieldMapping.values()) {
            Class<?> entity = mapping.getJoin().getEntityClass();
            assertAccepts(mapping, entity, mapping.getAttribute());
            if (mapping.isPair()) {
                assertAccepts(mapping, entity, mapping.getSecondAttribute());
            }
        }
    }

    /**
     * Only a date field may span a pair of columns; the pair is what makes a range cover the
     * whole dating rather than one of its ends.
     */
    @Test
    public void onlyDateFieldsSpanAPairOfColumns() {
        for (AipFieldMapping mapping : AipFieldMapping.values()) {
            if (mapping.isPair()) {
                assertEquals(AipFilterValueType.DATE, mapping.getValueType(),
                        mapping + " spans two columns but is not a date");
            }
        }
    }

    private static void assertAccepts(final AipFieldMapping mapping, final Class<?> entity, final String attribute) {
        Class<?> javaType = attributeType(entity, attribute);
        assertTrue(mapping.getValueType().accepts(javaType),
                mapping + " is declared " + mapping.getValueType() + " but " + entity.getSimpleName() + "."
                        + attribute + " is " + javaType.getSimpleName());
    }

    private static Class<?> attributeType(final Class<?> entity, final String attribute) {
        for (Class<?> cls = entity; cls != null; cls = cls.getSuperclass()) {
            try {
                Field field = cls.getDeclaredField(attribute);
                return field.getType();
            } catch (NoSuchFieldException e) {
                // continue up the hierarchy
            }
        }
        fail("no attribute '" + attribute + "' on " + entity.getName());
        return null;
    }
}
