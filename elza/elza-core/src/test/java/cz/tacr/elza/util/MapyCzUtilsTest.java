package cz.tacr.elza.util;

import cz.tacr.elza.utils.MapyCzUtils;
import org.junit.Assert;
import org.junit.Test;

public class MapyCzUtilsTest {

    @Test
    public void testDetekceSouradnice() {
        Assert.assertTrue(MapyCzUtils.isFromMapyCz("49.7474556N, 13.3776397E"));
        Assert.assertTrue(MapyCzUtils.isFromMapyCz("13.3776397E, 49.7474556N"));
        Assert.assertTrue(MapyCzUtils.isFromMapyCz("13.3776397e, 49.7474556n"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz(""));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("asdfadsf asdf asd fasf"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("POINT(1.1 2.2)"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("POINT(-73.9617828 40.7862706)"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("49.7474556E, 13.3776397E"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("49.7474556, 13.3776397"));
        Assert.assertFalse(MapyCzUtils.isFromMapyCz("49, 13"));
    }

    @Test
    public void testKonverzeSouradnice() {

        // ok

        Assert.assertEquals("POINT(13.3776397 49.7474556)", MapyCzUtils.transformToWKT("49.7474556N, 13.3776397E"));
        Assert.assertEquals("POINT(13.3776397 49.7474556)", MapyCzUtils.transformToWKT("13.3776397E, 49.7474556N"));
        Assert.assertEquals("POINT(-73.9617828 40.7862706)", MapyCzUtils.transformToWKT("40.7862706N, 73.9617828W"));
        Assert.assertEquals("POINT(-47.8344211 -15.7978378)", MapyCzUtils.transformToWKT("15.7978378S, 47.8344211W"));
        Assert.assertEquals("POINT(151.2912167 -33.6399081)", MapyCzUtils.transformToWKT("33.6399081S, 151.2912167E"));

        // fail

        try {
            MapyCzUtils.transformToWKT("asdfadsf asdf asd fasf");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            MapyCzUtils.transformToWKT("49.7474556E, 13.3776397E");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            MapyCzUtils.transformToWKT("49.7474556E, 13.3776397");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

}
