package com.cyngn.vertx.web.handler;

import com.cyngn.vertx.web.RestApi;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.WebTestBase;
import org.junit.Test;

/**
 * @author truelove@cyngn.com (Jeremy Truelove) 9/5/15
 */
public class RequestIdResponseHandlerTest extends WebTestBase {

    @Test
    public void testRequestId() throws Exception {
        router.route().handler(RequestIdResponseHandler.create());
        router.route().handler(rc -> {
            rc.response().end();
        });
        testRequest(HttpMethod.GET, "/", req -> req.putHeader(RestApi.X_REQUEST_ID, "aTestId"),
        resp -> {
            String idHeader = resp.headers().get(RestApi.X_REQUEST_ID);
            assertNotNull(idHeader);
            assertEquals("aTestId", idHeader);
        },
        200, "OK", null);
    }

    @Test
    public void testRequestIdAbsent() throws Exception {
        router.route().handler(RequestIdResponseHandler.create());
        router.route().handler(rc -> {
            rc.response().end();
        });
        testRequest(HttpMethod.GET, "/", null,
        resp -> {
            String idHeader = resp.headers().get(RestApi.X_REQUEST_ID);
            assertNull(idHeader);
        },
        200, "OK", null);
    }
}
