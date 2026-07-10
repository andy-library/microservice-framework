package com.microservice.framework.common.id;

import java.util.List;

/**
 * ID 生成器 SPI 接口
 * <p>
 * 定义分布式 ID 生成的标准契约，默认实现为雪花算法。
 * <p>
 * 应用可通过 Spring SPI 替换默认实现，只需注册一个
 * 实现 {@link IdGenerator} 的 Bean 即可覆盖默认配置。
 *
 * @author Andy Yang
 */
public interface IdGenerator {

    /**
     * 生成单个分布式 ID
     *
     * @return 全局唯一的长整型 ID
     */
    long generate();

    /**
     * 批量生成分布式 ID
     *
     * @param count 需要生成的 ID 数量，必须大于 0
     * @return 包含 count 个全局唯一 ID 的列表
     */
    List<Long> batchGenerate(int count);
}
