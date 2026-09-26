package br.com.sprint1.challenge.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Minimal structured JSON layout ensuring every log line is valid JSON.
 * Applies regex masking to sensitive fields and escapes quotes/newlines/control characters.
 * ponytail: upgrade to logstash-logback-encoder when multi-appender or custom field indexing needed.
 */
public class JsonLogLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)((password|senha|secret|token|hashed_password|hashedPassword|cpf)\\s*[:=]\\s*\"?)[^\",\\s}]+"
    );

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"timestamp\":\"");
        sb.append(ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
        sb.append("\",\"level\":\"");
        sb.append(event.getLevel().toString());
        sb.append("\",\"logger\":\"");
        escapeJson(event.getLoggerName(), sb);
        sb.append("\",\"thread\":\"");
        escapeJson(event.getThreadName(), sb);
        sb.append("\",\"message\":\"");

        String formattedMessage = event.getFormattedMessage();
        if (formattedMessage != null) {
            String masked = SENSITIVE_PATTERN.matcher(formattedMessage).replaceAll("$1***");
            escapeJson(masked, sb);
        }

        sb.append("\",\"exception\":\"");
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            String exStr = ThrowableProxyUtil.asString(tp);
            escapeJson(exStr, sb);
        }
        sb.append("\"}\n");

        return sb.toString();
    }

    private void escapeJson(String input, StringBuilder sb) {
        if (input == null) return;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }
}
