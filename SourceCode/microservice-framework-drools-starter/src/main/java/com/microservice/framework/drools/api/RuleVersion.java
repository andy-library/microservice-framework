package com.microservice.framework.drools.api;

import java.util.Objects;

/**
 * 规则版本信息
 * <p>
 * 不可变对象，封装规则包的版本元数据。
 * 用于追踪规则包的来源、版本和包含的规则数量，
 * 便于规则版本管理和运行治理。
 *
 * @author Andy Yang
 */
public final class RuleVersion {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final int ruleCount;

    /**
     * 创建规则版本信息
     *
     * @param groupId    规则包的组标识
     * @param artifactId 规则包的构件标识
     * @param version    规则包版本号
     * @param ruleCount  规则包中包含的规则数量
     */
    public RuleVersion(String groupId, String artifactId, String version, int ruleCount) {
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        if (ruleCount < 0) {
            throw new IllegalArgumentException("ruleCount must be at least 0, but was: " + ruleCount);
        }
        this.ruleCount = ruleCount;
    }

    /**
     * 获取规则包的组标识
     *
     * @return 组标识
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 获取规则包的构件标识
     *
     * @return 构件标识
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * 获取规则包版本号
     *
     * @return 版本号
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取规则包中包含的规则数量
     *
     * @return 规则数量
     */
    public int getRuleCount() {
        return ruleCount;
    }

    /**
     * 获取规则包的完整坐标标识
     * <p>
     * 格式为 groupId:artifactId:version
     *
     * @return 完整坐标标识
     */
    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public String toString() {
        return "RuleVersion{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", ruleCount=" + ruleCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RuleVersion)) {
            return false;
        }
        RuleVersion that = (RuleVersion) o;
        return ruleCount == that.ruleCount
                && groupId.equals(that.groupId)
                && artifactId.equals(that.artifactId)
                && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + ruleCount;
        return result;
    }
}
