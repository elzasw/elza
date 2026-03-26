package cz.tacr.elza.util;

import cz.tacr.elza.utils.MapyCzUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapyCzUtilsTest {

    @Test
    public void testDetekceSouradnice() {
    	Assertions.assertTrue(MapyCzUtils.isFromMapyCz("49.7474556N, 13.3776397E"));
    	Assertions.assertTrue(MapyCzUtils.isFromMapyCz("13.3776397E, 49.7474556N"));
    	Assertions.assertTrue(MapyCzUtils.isFromMapyCz("13.3776397e, 49.7474556n"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz(""));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("asdfadsf asdf asd fasf"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("POINT(1.1 2.2)"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("POINT(-73.9617828 40.7862706)"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("49.7474556E, 13.3776397E"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("49.7474556, 13.3776397"));
    	Assertions.assertFalse(MapyCzUtils.isFromMapyCz("49, 13"));
    }

    @Test
    public void testKonverzeSouradnice() {

        // ok

    	Assertions.assertEquals("POINT(13.3776397 49.7474556)", MapyCzUtils.transformToWKT("49.7474556N, 13.3776397E"));
    	Assertions.assertEquals("POINT(13.3776397 49.7474556)", MapyCzUtils.transformToWKT("13.3776397E, 49.7474556N"));
    	Assertions.assertEquals("POINT(-73.9617828 40.7862706)", MapyCzUtils.transformToWKT("40.7862706N, 73.9617828W"));
    	Assertions.assertEquals("POINT(-47.8344211 -15.7978378)", MapyCzUtils.transformToWKT("15.7978378S, 47.8344211W"));
    	Assertions.assertEquals("POINT(151.2912167 -33.6399081)", MapyCzUtils.transformToWKT("33.6399081S, 151.2912167E"));

        // fail

        try {
            MapyCzUtils.transformToWKT("asdfadsf asdf asd fasf");
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            MapyCzUtils.transformToWKT("49.7474556E, 13.3776397E");
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            MapyCzUtils.transformToWKT("49.7474556E, 13.3776397");
            Assertions.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

}
