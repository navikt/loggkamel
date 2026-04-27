package no.nav.sikkerhetstjenesten.loggkamel.observability;

import io.prometheus.metrics.core.metrics.Counter;

public final class Metrics {

    public static final Counter LOG_GROUP_CONSUMED = Counter.builder()
            .name("log_groups_consumed_total")
            .labelNames("teknologi")
            .help("Number of Log Group files consumed").register();

    public static final Counter INTERMEDIATE_LOG_LINE_ACTION = Counter.builder()
            .name("intermediate_log_lines_total")
            .labelNames("action")
            .help("Number of Log Line files published").register();

    public static final Counter ENRICHED_LOG_PUBLISHED = Counter.builder()
            .name("enriched_logs_published_total")
            .help("Number of enriched auditlogg lines published").register();

    public static final Counter LOG_GROUP_PUBLISHED_TO_BACKOUT_QUEUE = Counter.builder()
            .name("log_groups_published_to_backout_queue_total")
            .labelNames("teknologi", "queue")
            .help("Number of log group files published to backout queues").register();

    public static final Counter LOG_LINE_PUBLISHED_TO_BACKOUT_QUEUE = Counter.builder()
            .name("log_lines_published_to_backout_queue_total")
            .labelNames("teknologi", "queue")
            .help("Number of log line files published to backout queues").register();
}
