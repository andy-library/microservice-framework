package com.microservice.framework.json.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 统一 JSON 序列化/反序列化抽象接口
 * <p>
 * 所有 JSON 操作必须通过此接口进行，禁止直接使用底层实现（Jackson/Fastjson2）。
 * 框架提供 {@link com.microservice.framework.json.jackson.JacksonJsonCodec} 作为默认实现，
 * 可通过 {@code framework.json.provider=fastjson2} 切换为 Fastjson2 实现。
 *
 * @author Andy Yang
 */
public interface JsonCodec {

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param value 要序列化的对象，不允许为 null
     * @return JSON 字符串
     * @throws JsonCodecException 如果序列化失败或参数为 null
     */
    String serialize(Object value) throws JsonCodecException;

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param json  JSON 字符串，不允许为 null
     * @param type  目标类型，不允许为 null
     * @param <T>   目标类型泛型参数
     * @return 反序列化后的对象
     * @throws JsonCodecException 如果反序列化失败、参数为 null 或安全限制触发
     */
    <T> T deserialize(String json, Class<T> type) throws JsonCodecException;

    /**
     * 将 JSON 字符串反序列化为泛型类型对象
     * <p>
     * 用于反序列化带有泛型参数的类型，如 {@code List<User>}、{@code Map<String, Object>} 等。
     *
     * @param json       JSON 字符串，不允许为 null
     * @param typeRef    泛型类型引用，不允许为 null
     * @param <T>        目标类型泛型参数
     * @return 反序列化后的对象
     * @throws JsonCodecException 如果反序列化失败、参数为 null 或安全限制触发
     */
    <T> T deserialize(String json, JsonTypeReference<T> typeRef) throws JsonCodecException;

    /**
     * 将对象序列化为 JSON 字符串并写入输出流
     *
     * @param value 要序列化的对象，不允许为 null
     * @param out   目标输出流，不允许为 null
     * @throws JsonCodecException 如果序列化或写入失败
     */
    void serializeToStream(Object value, OutputStream out) throws JsonCodecException;

    /**
     * 从输入流反序列化为指定类型的对象
     *
     * @param in    JSON 输入流，不允许为 null
     * @param type  目标类型，不允许为 null
     * @param <T>   目标类型泛型参数
     * @return 反序列化后的对象
     * @throws JsonCodecException 如果反序列化失败或安全限制触发
     */
    <T> T deserializeFromStream(InputStream in, Class<T> type) throws JsonCodecException;

    /**
     * 将 JSON 字符串反序列化为对象列表
     *
     * @param json  JSON 字符串，不允许为 null
     * @param type  列表元素类型，不允许为 null
     * @param <T>   列表元素类型泛型参数
     * @return 反序列化后的对象列表
     * @throws JsonCodecException 如果反序列化失败或安全限制触发
     */
    <T> List<T> deserializeList(String json, Class<T> type) throws JsonCodecException;

    /**
     * 将 JSON 字符串反序列化为字符串键映射
     *
     * @param json  JSON 字符串，不允许为 null
     * @param valueType  映射值类型，不允许为 null
     * @param <V>   映射值类型泛型参数
     * @return 反序列化后的映射
     * @throws JsonCodecException 如果反序列化失败或安全限制触发
     */
    <V> Map<String, V> deserializeMap(String json, Class<V> valueType) throws JsonCodecException;

    /**
     * 返回当前 JSON 实现的名称（如 "jackson" 或 "fastjson2"）
     *
     * @return 实现名称
     */
    String getImplementationName();
}
