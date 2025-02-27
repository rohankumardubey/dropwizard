package io.dropwizard.request.logging;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Appender;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogbackAccessRequestLogTest {

    @SuppressWarnings("unchecked")
    private final Appender<IAccessEvent> appender = mock(Appender.class);
    private final LogbackAccessRequestLog requestLog = new LogbackAccessRequestLog();

    private final Request request = mock(Request.class);
    private final Response response = mock(Response.class);
    private final HttpChannelState channelState = mock(HttpChannelState.class);

    @BeforeEach
    void setUp() throws Exception {
        when(channelState.isInitial()).thenReturn(true);

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getTimeStamp()).thenReturn(TimeUnit.SECONDS.toMillis(1353042047));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test/things?yay");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        when(request.getHttpChannelState()).thenReturn(channelState);

        MetaData.Response metaData = mock(MetaData.Response.class);
        when(metaData.getStatus()).thenReturn(200);

        when(response.getCommittedMetaData()).thenReturn(metaData);

        HttpChannel channel = mock(HttpChannel.class);
        when(channel.getBytesWritten()).thenReturn(8290L);

        when(response.getHttpChannel()).thenReturn(channel);

        HttpFields.Mutable responseFields = HttpFields.build();
        responseFields.add("Testheader", "Testvalue1");
        responseFields.add("Testheader", "Testvalue2");
        when(response.getHttpFields()).thenReturn(responseFields);

        requestLog.addAppender(appender);

        requestLog.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        requestLog.stop();
    }

    @Test
    void logsRequestsToTheAppender() {
        final IAccessEvent event = logAndCapture();

        assertThat(event.getRemoteAddr()).isEqualTo("10.0.0.1");
        assertThat(event.getMethod()).isEqualTo("GET");
        assertThat(event.getRequestURI()).isEqualTo("/test/things?yay");
        assertThat(event.getProtocol()).isEqualTo("HTTP/1.1");

        assertThat(event.getStatusCode()).isEqualTo(200);
        assertThat(event.getContentLength()).isEqualTo(8290L);
    }

    @Test
    void combinesHeaders() {
        final IAccessEvent event = logAndCapture();

        assertThat(event.getResponseHeaderMap()).containsEntry("Testheader", "Testvalue1,Testvalue2");
    }

    private IAccessEvent logAndCapture() {
        requestLog.log(request, response);

        final ArgumentCaptor<IAccessEvent> captor = ArgumentCaptor.forClass(IAccessEvent.class);
        verify(appender, timeout(1000)).doAppend(captor.capture());

        return captor.getValue();
    }
}
