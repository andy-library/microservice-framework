package com.microservice.framework.database.api;

/**
 * Outbox 事件发布器
 * <p>
 * 实现事务性 Outbox 模式，确保业务操作与事件发布的原子性。
 * <p>
 * 设计意图：
 * <ul>
 *   <li>解决"业务成功但事件未发布"或"事件重复发布"的一致性问题</li>
 *   <li>事件先写入数据库 Outbox 表（与业务同事务），再由后台线程可靠推送</li>
 *   <li>配合 TransactionTemplateFacade 使用，确保写入与业务操作同事务</li>
 * </ul>
 * <p>
 * 使用流程：
 * <ol>
 *   <li>在业务事务内调用 {@link #publish(String, String, String)} 将事件写入 Outbox 表</li>
 *   <li>后台线程调用 {@link #findUnpublished()} 获取待发布事件</li>
 *   <li>成功推送到消息中间件后调用 {@link #markPublished(String)} 标记事件已发布</li>
 * </ol>
 *
 * @author Andy Yang
 */
public interface OutboxPublisher {

    /**
     * 发布事件到 Outbox 表
     * <p>
     * 在业务事务内调用，事件将写入数据库 Outbox 表，
     * 保证与业务操作的原子性。
     *
     * @param aggregateType 聚合类型（如 "Order"、"Payment"）
     * @param aggregateId   聚合 ID（如订单号、支付流水号）
     * @param eventType     事件类型（如 "CREATED"、"PAID"）
     * @param payload       事件内容（JSON 格式）
     */
    void publish(String aggregateType, String aggregateId, String eventType, String payload);

    /**
     * 标记事件已成功发布到消息中间件
     * <p>
     * 在消息中间件确认接收后调用，防止重复推送。
     *
     * @param eventId Outbox 表中的事件 ID
     */
    void markPublished(String eventId);

    /**
     * 查找所有未发布的事件
     * <p>
     * 由后台定时任务调用，获取 Outbox 表中尚未推送到消息中间件的事件列表。
     *
     * @return 未发布事件列表，每个事件包含 ID、聚合类型、聚合 ID、事件类型和 payload
     */
    java.util.List<OutboxEvent> findUnpublished();

    /**
     * Outbox 事件数据模型
     * <p>
     * 表示 Outbox 表中一条待发布或已发布的事件记录。
     */
    final class OutboxEvent {

        private final String eventId;
        private final String aggregateType;
        private final String aggregateId;
        private final String eventType;
        private final String payload;
        private final boolean published;
        private final java.time.Instant createdAt;

        /**
         * 创建 Outbox 事件
         *
         * @param eventId       事件 ID
         * @param aggregateType 聚合类型
         * @param aggregateId   聚合 ID
         * @param eventType     事件类型
         * @param payload       事件内容
         * @param published     是否已发布
         * @param createdAt     创建时间
         */
        public OutboxEvent(String eventId, String aggregateType, String aggregateId,
                           String eventType, String payload, boolean published,
                           java.time.Instant createdAt) {
            this.eventId = eventId;
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.payload = payload;
            this.published = published;
            this.createdAt = createdAt;
        }

        public String getEventId() {
            return eventId;
        }

        public String getAggregateType() {
            return aggregateType;
        }

        public String getAggregateId() {
            return aggregateId;
        }

        public String getEventType() {
            return eventType;
        }

        public String getPayload() {
            return payload;
        }

        public boolean isPublished() {
            return published;
        }

        public java.time.Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OutboxEvent that = (OutboxEvent) o;
            return java.util.Objects.equals(eventId, that.eventId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(eventId);
        }

        @Override
        public String toString() {
            return "OutboxEvent{" +
                    "eventId='" + eventId + '\'' +
                    ", aggregateType='" + aggregateType + '\'' +
                    ", aggregateId='" + aggregateId + '\'' +
                    ", eventType='" + eventType + '\'' +
                    ", published=" + published +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }
}
