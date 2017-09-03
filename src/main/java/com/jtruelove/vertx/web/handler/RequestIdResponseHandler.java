package com.jtruelove.vertx.web.handler;

import com.jtruelove.vertx.web.RestApi;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang.StringUtils;

/**
 * Tags a response with the request id we associated to the request if it had the header present.
 *
 * @author truelove@cyngn.com (Jeremy Truelove) 09/05/15
 */
public class RequestIdResponseHandler implements Handler<RoutingContext> {

  private RequestIdResponseHandler(){}

  /**
   * Get a RequestIdResponseHandler
   *
   * @return reference to created handler
   */
  public static RequestIdResponseHandler create() { return new RequestIdResponseHandler(); }

  @Override
  public void handle(RoutingContext ctx) {
    ctx.addHeadersEndHandler(aVoid -> {
        // grab the header from request and add to response if present
        String requestId = ctx.request().getHeader(RestApi.X_REQUEST_ID);
        if (StringUtils.isNotEmpty(requestId)) {
          ctx.response().putHeader(RestApi.X_REQUEST_ID, requestId);
        }
      });
      ctx.next();
    }
  }
