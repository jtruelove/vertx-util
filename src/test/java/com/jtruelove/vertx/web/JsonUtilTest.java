package com.jtruelove.vertx.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author truelove@cyngn.com (Jeremy Truelove) 2/4/15
 */
public class JsonUtilTest {

    static class Foo {
        @JsonProperty
        public String bar;

        @JsonProperty
        public int testField;

        @JsonIgnore
        public String dontTouch;

        public Foo() {
        }
    }

    class Bar {
        public String foo;
    }

    @Test
    public void testJsonDeserialize() {
        String testStr = "{\"bar\":\"testStr\",\"testField\":5}";

        Foo f = JsonUtil.parseJsonToObject(testStr, Foo.class);

        assertEquals("testStr", f.bar);
        assertEquals(5, f.testField);
    }

    @Test
    public void testInvalidJsonDeserialize() {
        Foo f = JsonUtil.parseJsonToObject((String) null, Foo.class);

        assertNull(f);
    }

    @Test
    public void testJsonSerialize() {
        Foo f = new Foo();
        f.bar = "testStr";
        f.testField = 5;
        f.dontTouch = "notMe";

        String testStr = JsonUtil.getJsonForObject(f);

        JsonObject jsonObject = new JsonObject(testStr);
        assertTrue(jsonObject.getString("bar").equals(f.bar));
        assertTrue(!jsonObject.containsKey("dontTouch"));

        assertEquals(testStr, jsonObject.toString());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBadObjectJsonSerialize() {
        String testStr = JsonUtil.getJsonForObject(null);
    }

    @Test
    public void testInvalidJsonDeserializeBytes() {
        String badJson = "{[}";
        Foo f = JsonUtil.parseJsonToObject(badJson.getBytes(), Foo.class);
        assertNull(f);
    }

    @Test
    public void testNullJsonDeserializeBytes() {
        Foo f = JsonUtil.parseJsonToObject((byte[])null, Foo.class);
        assertNull(f);
    }

    @Test
    public void testJavaTimeSerialization(){
        LocalDateTime then = LocalDateTime.now();

        ZonedDateTime utcZdt = ZonedDateTime.of(then, ZoneId.of("UTC"));
        String utcZdtJson = JsonUtil.getJsonForObject(utcZdt);
        ZonedDateTime utcZdtResurrected = JsonUtil.parseJsonToObject(utcZdtJson, ZonedDateTime.class);
        assertTrue(utcZdt.compareTo(utcZdtResurrected) == 0);

        ZonedDateTime gmtZdt = ZonedDateTime.of(then, ZoneId.of("GMT"));
        String gmtZdtJson = JsonUtil.getJsonForObject(gmtZdt);
        ZonedDateTime gmtZdtResurrected = JsonUtil.parseJsonToObject(gmtZdtJson, ZonedDateTime.class);
        assertTrue(gmtZdt.compareTo(gmtZdtResurrected) != 0);//our default is UTC, and UTC != GMT
        assertTrue(utcZdtResurrected.compareTo(gmtZdtResurrected) == 0);//however, both resurrected values match
    }
}
