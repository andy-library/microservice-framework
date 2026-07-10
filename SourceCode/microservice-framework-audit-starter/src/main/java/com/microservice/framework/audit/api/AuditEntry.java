package com.microservice.framework.audit.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * 审计条目
 * <p>
 * 不可变对象，封装单条审计事件的完整信息。
 * 通过 checksum 字段实现防篡改校验，确保审计记录的完整性。
 * <p>
 * checksum 的计算方式：
 * 将 eventType + operatorId + targetId + action + detail + timestamp 拼接后
 * 使用配置的算法（默认 SHA-256）计算哈希值。
 *
 * @author Andy Yang
 */
public final class AuditEntry {

    private final String id;
    private final String eventType;
    private final String operatorId;
    private final String targetId;
    private final String action;
    private final String detail;
    private final Instant timestamp;
    private final String checksum;

    /**
     * 创建审计条目（自动计算 checksum）
     *
     * @param id         审计条目唯一标识
     * @param eventType  事件类型
     * @param operatorId 操作者 ID
     * @param targetId   目标对象 ID
     * @param action     操作类型
     * @param detail     详细描述
     * @param timestamp  事件时间戳
     * @param algorithm  checksum 计算算法（如 SHA-256）
     */
    public AuditEntry(String id, String eventType, String operatorId,
                      String targetId, String action, String detail,
                      Instant timestamp, String algorithm) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.operatorId = operatorId;
        this.targetId = targetId;
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.detail = detail;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.checksum = computeChecksum(algorithm);
    }

    /**
     * 从存储恢复审计条目（使用预计算的 checksum）
     * <p>
     * 用于从持久化存储中恢复审计条目时使用，
     * checksum 已预先计算，无需重新计算。
     *
     * @param id         审计条目唯一标识
     * @param eventType  事件类型
     * @param operatorId 操作者 ID
     * @param targetId   目标对象 ID
     * @param action     操作类型
     * @param detail     详细描述
     * @param timestamp  事件时间戳
     * @param checksum   预计算的 checksum
     * @return 从存储恢复的审计条目
     */
    public static AuditEntry restoreFromStorage(String id, String eventType, String operatorId,
                                                 String targetId, String action, String detail,
                                                 Instant timestamp, String checksum) {
        return new AuditEntry(id, eventType, operatorId,
                targetId, action, detail, timestamp, checksum, true);
    }

    /**
     * 内部构造方法，直接指定 checksum
     */
    private AuditEntry(String id, String eventType, String operatorId,
                       String targetId, String action, String detail,
                       Instant timestamp, String checksum, boolean skipCompute) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.operatorId = operatorId;
        this.targetId = targetId;
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.detail = detail;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.checksum = Objects.requireNonNull(checksum, "checksum must not be null");
    }

    /**
     * 计算 checksum
     * <p>
     * 将关键字段拼接后使用指定算法计算哈希值。
     *
     * @param algorithm 哈希算法名称
     * @return 计算得到的 checksum
     */
    private String computeChecksum(String algorithm) {
        String raw = eventType
                + "|" + (operatorId != null ? operatorId : "")
                + "|" + (targetId != null ? targetId : "")
                + "|" + action
                + "|" + (detail != null ? detail : "")
                + "|" + timestamp.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported checksum algorithm: " + algorithm, e);
        }
    }

    /**
     * 验证 checksum 是否一致
     * <p>
     * 使用指定算法重新计算 checksum 并与当前值比较，
     * 验证审计条目是否被篡改。
     *
     * @param algorithm 哈希算法名称
     * @return checksum 是否一致（true 表示未被篡改）
     */
    public boolean verifyChecksum(String algorithm) {
        String recomputed = computeChecksum(algorithm);
        return recomputed.equals(checksum);
    }

    /**
     * 获取审计条目唯一标识
     *
     * @return 条目 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 获取操作者 ID
     *
     * @return 操作者 ID，不存在时返回空 Optional
     */
    public Optional<String> getOperatorId() {
        return Optional.ofNullable(operatorId);
    }

    /**
     * 获取目标对象 ID
     *
     * @return 目标对象 ID，不存在时返回空 Optional
     */
    public Optional<String> getTargetId() {
        return Optional.ofNullable(targetId);
    }

    /**
     * 获取操作类型
     *
     * @return 操作类型
     */
    public String getAction() {
        return action;
    }

    /**
     * 获取详细描述
     *
     * @return 详细描述，不存在时返回空 Optional
     */
    public Optional<String> getDetail() {
        return Optional.ofNullable(detail);
    }

    /**
     * 获取事件时间戳
     *
     * @return 时间戳
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * 获取防篡改 checksum
     *
     * @return checksum 值
     */
    public String getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "AuditEntry{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", operatorId='" + operatorId + '\'' +
                ", targetId='" + targetId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", checksum='" + checksum + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditEntry)) {
            return false;
        }
        AuditEntry that = (AuditEntry) o;
        return id.equals(that.id)
                && eventType.equals(that.eventType)
                && Objects.equals(operatorId, that.operatorId)
                && Objects.equals(targetId, that.targetId)
                && action.equals(that.action)
                && Objects.equals(detail, that.detail)
                && timestamp.equals(that.timestamp)
                && checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + eventType.hashCode();
        result = 31 * result + (operatorId != null ? operatorId.hashCode() : 0);
        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
        result = 31 * result + action.hashCode();
        result = 31 * result + (detail != null ? detail.hashCode() : 0);
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + checksum.hashCode();
        return result;
    }
}
