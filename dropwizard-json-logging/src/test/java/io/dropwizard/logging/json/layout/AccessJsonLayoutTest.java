package io.dropwizard.logging.json.layout;

import ch.qos.logback.access.common.spi.IAccessEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.json.AccessAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

class AccessJsonLayoutTest {

    private String remoteHost = "nw-4.us.crawl.io";
    private String serverName = "sw-2.us.api.example.io";
    private String timestamp = "2018-01-01T14:35:21.000+0000";
    private String uri = "/test/users";
    private String query = "?age=22&city=LA";
    private String pathQuery = uri + query;
    private String url = "GET /test/users?age=22&city=LA HTTP/1.1";
    private String userAgent = "Mozilla/5.0";
    private Map<String, String> requestHeaders;
    private Map<String, String> responseHeaders;
    private String responseContent = "{\"message\":\"Hello, Crawler!\"}";
    private String remoteAddress = "192.168.52.15";
    private IAccessEvent event = Mockito.mock(IAccessEvent.class);

    private TimestampFormatter timestampFormatter = new TimestampFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ", ZoneId.of("UTC"));
    private ObjectMapper objectMapper = Jackson.newObjectMapper();
    private JsonFormatter jsonFormatter = new JsonFormatter(objectMapper, false, true);
    private Set<AccessAttribute> includes = EnumSet.of(AccessAttribute.REMOTE_ADDRESS,
        AccessAttribute.REMOTE_USER, AccessAttribute.REQUEST_TIME, AccessAttribute.REQUEST_URI,
        AccessAttribute.STATUS_CODE, AccessAttribute.METHOD, AccessAttribute.PROTOCOL, AccessAttribute.CONTENT_LENGTH,
        AccessAttribute.USER_AGENT, AccessAttribute.TIMESTAMP);
    private AccessJsonLayout accessJsonLayout = new AccessJsonLayout(jsonFormatter, timestampFormatter,
        includes, Collections.emptyMap(), Collections.emptyMap());

    @BeforeEach
    void setUp() {
        requestHeaders = Map.of(
                "Host", "api.example.io",
                "User-Agent", userAgent);
        responseHeaders = Map.of(
                "Content-Type", "application/json",
                "Transfer-Encoding", "chunked");
        when(event.getTimeStamp()).thenReturn(1514817321000L);
        when(event.getContentLength()).thenReturn(78L);
        when(event.getLocalPort()).thenReturn(8080);
        when(event.getMethod()).thenReturn("GET");
        when(event.getProtocol()).thenReturn("HTTP/1.1");
        when(event.getRequestContent()).thenReturn("");
        when(event.getRemoteAddr()).thenReturn(remoteAddress);
        when(event.getRemoteUser()).thenReturn("john");
        when(event.getRequestHeaderMap()).thenReturn(requestHeaders);
        when(event.getRequestParameterMap()).thenReturn(Collections.emptyMap());
        when(event.getElapsedTime()).thenReturn(100L);
        when(event.getRequestURI()).thenReturn(uri);
        when(event.getQueryString()).thenReturn(query);
        when(event.getRequestURL()).thenReturn(url);
        when(event.getRemoteHost()).thenReturn(remoteHost);
        when(event.getResponseContent()).thenReturn(responseContent);
        when(event.getResponseHeaderMap()).thenReturn(responseHeaders);
        when(event.getServerName()).thenReturn(serverName);
        when(event.getStatusCode()).thenReturn(200);
        when(event.getRequestHeader("User-Agent")).thenReturn(userAgent);
        accessJsonLayout.setIncludes(includes);
    }

    @Test
    void testProducesDefaultJsonMap() {
        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress));
    }

    @Test
    void testDisableRemoteAddress() {
        includes.remove(AccessAttribute.REMOTE_ADDRESS);
        accessJsonLayout.setIncludes(includes);

        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent));
    }

    @Test
    void testDisableTimestamp() {
        includes.remove(AccessAttribute.TIMESTAMP);
        accessJsonLayout.setIncludes(includes);

        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress));
    }

    @Test
    void testEnableSpecificResponseHeader() {
        accessJsonLayout.setResponseHeaders(Collections.singleton("transfer-encoding"));

        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress),
            entry("responseHeaders", Collections.singletonMap("Transfer-Encoding", "chunked")));
    }

    @Test
    void testEnableSpecificRequestHeader() {
        accessJsonLayout.setRequestHeaders(Collections.singleton("user-agent"));

        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress),
            entry("headers", Collections.singletonMap("User-Agent", userAgent)));
    }

    @Test
    void testEnableEverything() {
        accessJsonLayout.setIncludes(EnumSet.allOf(AccessAttribute.class));
        accessJsonLayout.setRequestHeaders(Set.of("Host", "User-Agent"));
        accessJsonLayout.setResponseHeaders(Set.of("Transfer-Encoding", "Content-Type"));

        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress),
            entry("responseHeaders", this.responseHeaders),
            entry("responseContent", responseContent),
            entry("port", 8080), entry("requestContent", ""),
            entry("headers", this.requestHeaders),
            entry("remoteHost", remoteHost), entry("url", url),
            entry("serverName", serverName),
            entry("pathQuery", pathQuery));
    }

    @Test
    void testAddAdditionalFields() {
        final Map<String, Object> additionalFields = Map.of(
                "serviceName", "user-service",
                "serviceVersion", "1.2.3");
        accessJsonLayout = new AccessJsonLayout(jsonFormatter, timestampFormatter, includes, Collections.emptyMap(),
                additionalFields);
        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remoteUser", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("requestTime", 100L), entry("contentLength", 78L),
            entry("userAgent", userAgent), entry("remoteAddress", remoteAddress),
            entry("serviceName", "user-service"), entry("serviceVersion", "1.2.3"));
    }

    @Test
    void testCustomFieldNames() {
        final Map<String, String> customFieldNames = Map.of(
                "remoteUser", "remote_user",
                "userAgent", "user_agent",
                "remoteAddress", "remote_address",
                "contentLength", "content_length",
                "requestTime", "request_time");
        accessJsonLayout = new AccessJsonLayout(jsonFormatter, timestampFormatter, includes, customFieldNames, Collections.emptyMap());
        assertThat(accessJsonLayout.toJsonMap(event)).containsOnly(
            entry("timestamp", timestamp), entry("remote_user", "john"),
            entry("method", "GET"), entry("uri", uri),
            entry("protocol", "HTTP/1.1"), entry("status", 200),
            entry("request_time", 100L), entry("content_length", 78L),
            entry("user_agent", userAgent), entry("remote_address", remoteAddress));
    }

    @Test
    void testRequestAttributes() {
        final String attribute1 = "attribute1";
        final String attribute2 = "attribute2";
        final String attribute3 = "attribute3";

        final Map<String, String> attributes =
            Map.of(
                attribute1, "value1",
                attribute2, "value2",
                attribute3, "value3");

        when(event.getAttribute(attribute1)).thenReturn(attributes.get(attribute1));
        when(event.getAttribute(attribute2)).thenReturn(attributes.get(attribute2));
        when(event.getAttribute(attribute3)).thenReturn(attributes.get(attribute3));

        accessJsonLayout.setRequestAttributes(attributes.keySet());
        assertThat(accessJsonLayout.toJsonMap(event))
            .containsEntry("requestAttributes", attributes);
    }

    @Test
    void testStartAndStop() {
        accessJsonLayout.start();
        assertThat(accessJsonLayout.isStarted()).isTrue();
        accessJsonLayout.stop();
        assertThat(accessJsonLayout.isStarted()).isFalse();
    }

    @Test
    void testRequestAttributesWithNull() {
        final String attribute1 = "attribute1";
        final String attribute2 = "attribute2";
        final String attribute3 = "attribute3";

        final Map<String, String> attributes = Map.of(
                attribute1, "value1",
                attribute2, "value2");

        when(event.getAttribute(attribute1)).thenReturn(attributes.get(attribute1));
        when(event.getAttribute(attribute2)).thenReturn(attributes.get(attribute2));
        when(event.getAttribute(attribute3)).thenReturn(null);

        accessJsonLayout.setRequestAttributes(Set.of(attribute1, attribute2, attribute3));
        assertThat(accessJsonLayout.toJsonMap(event))
            .containsEntry("requestAttributes", Map.of(attribute1, "value1", attribute2, "value2"));

    }

    @Test
    void testProducesCorrectJson() throws Exception {
        JsonNode json = objectMapper.readTree(accessJsonLayout.doLayout(event));
        assertThat(json).isNotNull();
        assertThat(json.get("timestamp").asText()).isEqualTo(timestamp);
        assertThat(json.get("remoteUser").asText()).isEqualTo("john");
        assertThat(json.get("method").asText()).isEqualTo("GET");
        assertThat(json.get("uri").asText()).isEqualTo(uri);
        assertThat(json.get("protocol").asText()).isEqualTo("HTTP/1.1");
        assertThat(json.get("status").asInt()).isEqualTo(200);
        assertThat(json.get("requestTime").asInt()).isEqualTo(100);
        assertThat(json.get("contentLength").asInt()).isEqualTo(78);
        assertThat(json.get("userAgent").asText()).isEqualTo(userAgent);
        assertThat(json.get("remoteAddress").asText()).isEqualTo(remoteAddress);
    }
}
