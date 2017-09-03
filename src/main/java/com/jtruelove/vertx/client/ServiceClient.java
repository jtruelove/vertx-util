package com.jtruelove.vertx.client;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * Service client for vertx services.

 * Uses {@link HttpClient} as underlying client
 *
 * @author asarda@cyngn.com (Ajay Sarda) on 8/25/15.
 */
public class ServiceClient {

    // configuration key constants
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String NUM_CONNECTIONS = "num_connections";
    public static final String COMPRESSION = "compression";
    public static final String APIS = "apis";
    public static final String API_NAME = "name";
    public static final String TIMEOUT = "timeout";
    public static final String SSL = "ssl";
    public static final String HEADERS = "headers";
    private static final long NO_TIMEOUT = 0L;

    // empty request.
    public static final String EMPTY_REQUEST = "";

    private Map<String, Long> apiTimeouts = new HashMap<>();

    // http client delegate
    private HttpClient client;

    // saving host and port for consumers
    private String host;
    private Integer port;
    private long timeout;
    private Map<String, String> headers;

    // private constructor to prohibit creating instances using constructor
    private ServiceClient() {}

    /**
     * Creates instance of {@link ServiceClient} from json configuration
     *
     * @param vertx  - reference to vertx instance.
     * @param config - Json configuration.
     * @return {@link ServiceClient} object
     */
    public static ServiceClient create(Vertx vertx, JsonObject config) {
        Builder builder = new Builder(vertx);

        if (config.containsKey(HOST)) {
            builder.withHost(config.getString(HOST));
        } else {
            throw new IllegalArgumentException("No host key defined in service client configuration");
        }

        if (config.containsKey(PORT)) {
            builder.withPort(config.getInteger(PORT));
        } else {
            throw new IllegalArgumentException("No port key defined in service client configuration");
        }

        if (config.containsKey(NUM_CONNECTIONS)) {
            builder.withNumConnections(config.getInteger(NUM_CONNECTIONS));
        }

        if (config.containsKey(COMPRESSION)) {
            builder.withCompression(config.getBoolean(COMPRESSION));
        }

        if (config.containsKey(TIMEOUT)) {
            builder.withTimeout(config.getLong(TIMEOUT));
        }

        if (config.containsKey(SSL)) {
            builder.withSsl(config.getBoolean(SSL));
        }

        if (config.containsKey(HEADERS)) {
            JsonObject headerConfig = config.getJsonObject(HEADERS, null);

            if (headerConfig != null) {
                Map<String, String> headers = new HashMap<>();
                headerConfig.forEach(entry -> {
                    headers.put(entry.getKey(), (String) entry.getValue());
                });
                builder.withHeaders(headers);
            }
        }

        if (config.containsKey(APIS)) {
            JsonArray apiArray = config.getJsonArray(APIS);

            for (int pos = 0; pos < apiArray.size(); pos++) {
                JsonObject apiObject = apiArray.getJsonObject(pos);
                String name = apiObject.getString(API_NAME);
                long timeout = apiObject.getLong(TIMEOUT, NO_TIMEOUT);
                builder.addApiTimeout(name, timeout);
            }
        }

        return builder.build();
    }

    private ServiceClient(HttpClient client, Map<String, Long> apiTimeouts, String host, Integer port, long timeout,
              Map<String, String> headers) {
        this.client = client;
        this.apiTimeouts = apiTimeouts;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.headers = headers;
    }

    /**
     * Gets the host name for which the service client is created.
     *
     * @return host name
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port for which the service client is created
     *
     * @return port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Fluent Builder class to create objects of {@link ServiceClient}
     */
    public static class Builder {
        private String host;
        private int port = 0;
        private boolean compression = HttpClientOptions.DEFAULT_TRY_USE_COMPRESSION;
        private int numConnections = HttpClientOptions.DEFAULT_MAX_POOL_SIZE;
        private long timeout = 0L;
        private final Vertx vertx;
        private boolean ssl;
        private Map<String, Long> apiTimeouts = new HashMap<>();
        private Map<String, String> headers;

        public Builder(Vertx vertx) {
            this.vertx = vertx;
        }

        /**
         * Builds the {@link ServiceClient} with specified parameters
         *
         * @return - instance of ServiceClient.
         */
        public ServiceClient build() {
            HttpClientOptions options = new HttpClientOptions();

            if (StringUtils.isNotBlank(host)) {
                options.setDefaultHost(host);
            } else {
                throw new IllegalArgumentException("missing host parameter");
            }

            if (port != 0) {
                options.setDefaultPort(port);
            } else {
                throw new IllegalArgumentException("missing port parameter");
            }

            options.setTryUseCompression(compression);
            options.setMaxPoolSize(numConnections);
            options.setSsl(ssl);

            // create the http client;
            HttpClient client = vertx.createHttpClient(options);

            return new ServiceClient(client, apiTimeouts, host, port, timeout, headers);
        }

        /**
         * Sets the hostname
         *
         * @param host - hostname associated with client.
         * @return - reference to Builder object
         */
        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the port
         *
         * @param port - port associated with client
         * @return - reference to Builder object.
         */
        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the compression
         *
         * @param compression - if compression is set
         * @return - reference to Builder object.
         */
        public Builder withCompression(boolean compression) {
            this.compression = compression;
            return this;
        }

        /**
         * Sets the number of connections in connection pool for the client
         *
         * @param numConnections - connection pool size.
         * @return - reference to Builder object.
         */
        public Builder withNumConnections(int numConnections) {
            this.numConnections = numConnections;
            return this;
        }

        /**
         * Sets the timeout for all api calls from client.
         * @param timeout - timeout in milliseconds. timeout with value 0 means no timeout.
         * @return - reference to Builder object.
         */
        public Builder withTimeout(long timeout) {
            if (timeout < 0L) {
                throw new IllegalArgumentException("Invalid timeout value: " + timeout);
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the ssl on client.
         *
         * Any request to secured http endpoint should have ssl set
         *
         * @param ssl - ssl enabled?
         * @return - reference to Builder object.
         */
        public Builder withSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        /**
         * Sets the headers to be sent on every api call from client.
         *
         * @param headers - map of string key, string value pairs.
         * @return - reference to Builder object.
         */
        public Builder withHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Adds the api to the builder
         *
         * @param name - api name used for subsequent usage to call api.
         * @param timeout - timeout in milliseconds. timeout with value 0 means no timeout.
         * @return - reference to Builder object.
         */
        public Builder addApiTimeout(String name, long timeout) {
            if (timeout < 0L) {
                throw new IllegalArgumentException("Invalid timeout value: " + timeout + " for api: " + name);
            }

            if (apiTimeouts.containsKey(name)) {
                throw new IllegalArgumentException("api by name " + name + " already added to builder");
            }

            apiTimeouts.put(name, timeout);
            return this;
        }
    }

    /**
     * Calls the service api
     *
     * @param httpMethod       - HTTP method for the request
     * @param path             - the absolute URI path
     * @param payload          - payload sent in the call.
     * @param timeout          - timeout in millis
     * @param responseHandler  -  response handler
     * @param exceptionHandler -  exception handler
     */
    public void call(HttpMethod httpMethod, String path, byte[] payload, long timeout, Handler<HttpClientResponse> responseHandler,
                     Handler<Throwable> exceptionHandler) {
        HttpClientRequest request = client.request(httpMethod, path, responseHandler)
                .exceptionHandler(exceptionHandler)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(payload.length))
                .write(Buffer.buffer(payload))
                .setTimeout(timeout);

        if (headers != null && headers.size() > 0) {
            headers.forEach((key, value) -> request.putHeader(key, value));
        }

        request.end();
    }

    /**
     * Calls the service api
     *
     * @param httpMethod       - HTTP method for the request
     * @param path             - the absolute URI path
     * @param serviceRequest   - service request object
     * @param responseHandler  -  response handler
     * @param exceptionHandler -  exception handler
     */
    public void call(HttpMethod httpMethod, String path, ServiceRequest serviceRequest, Handler<HttpClientResponse> responseHandler,
                     Handler<Throwable> exceptionHandler) {

        HttpClientRequest request = client.request(httpMethod, path, responseHandler)
                .exceptionHandler(exceptionHandler)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(
                        serviceRequest.hasPayload() ? serviceRequest.getPayload().length : EMPTY_REQUEST.length()));


        if (serviceRequest.hasTimeout()) {
            request.setTimeout(serviceRequest.getTimeout());
        }

        if (headers != null && headers.size() > 0) {
            headers.forEach((key, value) -> request.putHeader(key, value));
        }

        if (serviceRequest.hasHeaders()) {
            serviceRequest.getHeaders().forEach((key, value) -> request.putHeader(key, value));
        }

        request.end();
    }

    /**
     * Calls the service api
     *
     * @param httpMethod       - HTTP method for the request
     * @param path             - the absolute URI path
     * @param payload          - payload sent in the call.
     * @param responseHandler  -  response handler
     * @param exceptionHandler -  exception handler
     */
    public void call(HttpMethod httpMethod, String path, byte[] payload, Handler<HttpClientResponse> responseHandler,
                     Handler<Throwable> exceptionHandler) {
        HttpClientRequest request = client.request(httpMethod, path, responseHandler)
                .exceptionHandler(exceptionHandler)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(payload.length))
                .write(Buffer.buffer(payload));

        if (headers != null && headers.size() > 0) {
            headers.forEach((key, value) -> request.putHeader(key, value));
        }

        request.end();
    }

    /**
     * Calls the service api
     *
     * @param httpMethod       - HTTP method for the request
     * @param path             - the absolute URI path
     * @param responseHandler  -  response handler
     * @param exceptionHandler -  exception handler
     */
    public void call(HttpMethod httpMethod, String path, Handler<HttpClientResponse> responseHandler,
                     Handler<Throwable> exceptionHandler) {
        call(httpMethod, path, EMPTY_REQUEST.getBytes(), responseHandler, exceptionHandler);
    }

    /**
     * Calls the service api
     *
     * @param httpMethod       - HTTP method for the request
     * @param path             - the absolute URI path
     * @param timeout          - timeout in millis
     * @param responseHandler  -  response handler
     * @param exceptionHandler -  exception handler
     */
    public void call(HttpMethod httpMethod, String path, long timeout, Handler<HttpClientResponse> responseHandler,
                     Handler<Throwable> exceptionHandler) {
        call(httpMethod, path, EMPTY_REQUEST.getBytes(), timeout, responseHandler, exceptionHandler);
    }

    /**
     * Close the client. Closing will close down any pooled connections.
     * Clients should always be closed after use.
     */
    public void close() {
        client.close();
    }

    /**
     * Get timeout for the API.
     *
     * @param apiName - api name
     * @return timeout value
     */
    public Long getTimeout(String apiName) {
        Long timeout = apiTimeouts.get(apiName);

        return timeout == null ? this.timeout : timeout;
    }

}




