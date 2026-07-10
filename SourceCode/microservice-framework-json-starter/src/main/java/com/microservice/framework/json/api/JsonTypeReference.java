package com.microservice.framework.json.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 泛型类型引用，用于传递带有泛型参数的类型信息
 * <p>
 * 由于 Java 的类型擦除，{@code List<String>.class} 这样的泛型类型无法直接获取。
 * 使用此类可以在反序列化时保留完整的泛型类型信息：
 * <pre>
 * JsonTypeReference&lt;List&lt;User&gt;&gt; typeRef = new JsonTypeReference&lt;List&lt;User&gt;&gt;() {};
 * List&lt;User&gt; users = codec.deserialize(json, typeRef);
 * </pre>
 *
 * @author Andy Yang
 */
public abstract class JsonTypeReference<T> {

    private final Type type;

    /**
     * 子类必须通过匿名内部类方式创建实例，以保留泛型类型信息。
     * <p>
     * 正确用法：{@code new JsonTypeReference<List<User>() {}}
     * 错误用法：{@code new JsonTypeReference<List<User>()}（编译错误，抽象类不能直接实例化）
     */
    protected JsonTypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "JsonTypeReference must be created as an anonymous inner class "
                    + "to preserve generic type information, e.g.: new JsonTypeReference<List<User>() {}");
        }
        Type actualType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        this.type = actualType;
    }

    /**
     * 返回完整的泛型类型信息
     *
     * @return 泛型类型
     */
    public Type getType() {
        return type;
    }

    /**
     * 以字符串形式返回类型信息，便于调试和日志输出
     *
     * @return 类型描述字符串
     */
    @Override
    public String toString() {
        return type.toString();
    }
}
