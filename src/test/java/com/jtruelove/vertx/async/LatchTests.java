package com.jtruelove.vertx.async;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author truelove@cyngn.com (Jeremy Truelove) 10/15/14
 */
public class LatchTests {

    @Test
    public void countdownTest() {
        final List<Boolean> results = new ArrayList<>();
        Latch latch = new Latch(2, () -> results.add(true));
        latch.complete();
        latch.complete();

        assertTrue(results.size() == 1);
        assertTrue(results.get(0));

        latch.reset(3);
        results.clear();
        latch.complete();
        latch.complete();

        assertTrue(results.size() == 0);
    }

    @Test(expected = IllegalStateException.class)
    public void failCompleteCallsAfterCompletedTest() {
        final List<Boolean> results = new ArrayList<>();
        Latch latch = new Latch(2, () -> results.add(true));
        latch.complete();
        latch.complete();

        assertTrue(results.size() == 1);
        assertTrue(results.get(0));

        latch.complete();
    }
}
