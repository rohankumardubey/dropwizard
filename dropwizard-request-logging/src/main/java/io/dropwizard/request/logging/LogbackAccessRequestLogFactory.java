package io.dropwizard.request.logging;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.filter.NullLevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;
import io.dropwizard.request.logging.async.AsyncAccessEventAppenderFactory;
import io.dropwizard.request.logging.layout.LogbackAccessRequestLayoutFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.jetty.server.RequestLog;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A factory for creating {@link LogbackAccessRequestLog} instances.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>{@code appenders}</td>
 *         <td>a default {@link ConsoleAppenderFactory console} appender</td>
 *         <td>The set of {@link AppenderFactory appenders} to which requests will be logged.</td>
 *     </tr>
 * </table>
 */
@JsonTypeName("logback-access")
public class LogbackAccessRequestLogFactory implements RequestLogFactory<RequestLog> {

    @Valid
    @NotNull
    private List<AppenderFactory<IAccessEvent>> appenders = Collections.singletonList(
            new ConsoleAppenderFactory<>());

    @JsonProperty
    public List<AppenderFactory<IAccessEvent>> getAppenders() {
        return appenders;
    }

    @JsonProperty
    public void setAppenders(List<AppenderFactory<IAccessEvent>> appenders) {
        this.appenders = appenders;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return !appenders.isEmpty();
    }

    @Override
    public RequestLog build(String name) {
        final Logger logger = (Logger) LoggerFactory.getLogger("http.request");
        logger.setAdditive(false);

        final LoggerContext context = logger.getLoggerContext();

        final LogbackAccessRequestLog requestLog = new LogbackAccessRequestLog();

        final LevelFilterFactory<IAccessEvent> levelFilterFactory = new NullLevelFilterFactory<>();
        final AsyncAppenderFactory<IAccessEvent> asyncAppenderFactory = new AsyncAccessEventAppenderFactory();
        final LayoutFactory<IAccessEvent> layoutFactory = new LogbackAccessRequestLayoutFactory();

        for (AppenderFactory<IAccessEvent> output : appenders) {
            requestLog.addAppender(output.build(context, name, layoutFactory, levelFilterFactory, asyncAppenderFactory));
        }

        return requestLog;
    }
}
