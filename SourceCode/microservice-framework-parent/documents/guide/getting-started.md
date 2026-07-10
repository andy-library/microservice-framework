# Microservice Framework Parent 接入指南

Author: Andy Yang

## 1. 业务应用如何接入

业务应用必须继承 `microservice-framework-starter-parent`，不要继承根项目 `microservice-framework-parent`。

```xml
<parent>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-starter-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>
</parent>
```

`relativePath` 必须置空，确保业务应用从 Maven 仓库解析受控版本，而不是误连本地源码目录。

## 2. 引入 Starter

业务应用按能力引入 starter，不需要声明版本号。版本由 `microservice-framework-bom` 统一管理。

```xml
<dependencies>
    <dependency>
        <groupId>com.microservice.framework</groupId>
        <artifactId>microservice-framework-common-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.microservice.framework</groupId>
        <artifactId>microservice-framework-json-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.microservice.framework</groupId>
        <artifactId>microservice-framework-logging-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.microservice.framework</groupId>
        <artifactId>microservice-framework-web-starter</artifactId>
    </dependency>
</dependencies>
```

## 3. 版本治理规则

- Java 统一使用 21。
- Maven 必须为 3.9.0 或更高版本。
- Spring Boot 固定为 `3.3.13`。
- Spring Cloud 固定为 `2023.0.6`。
- 业务应用不得覆盖 `spring-boot.version` 和 `spring-cloud.version`。
- 禁止使用 `log4j:log4j`、`org.slf4j:slf4j-log4j12`、`com.alibaba:fastjson`。
- 禁止使用 `LATEST`、`RELEASE`、版本范围等动态版本。

## 4. 官方 Starter 清单

当前 BOM 管理 19 个官方 starter：

`common`、`json`、`logging`、`nacos`、`apollo`、`observability`、`database`、`redis`、`kafka`、`elasticsearch`、`async`、`xxl-job`、`web`、`feign`、`security`、`drools`、`audit`、`field-encryption`、`object-storage`。

## 5. 常见问题

### 是否可以覆盖框架定义的 Spring Boot 或 Spring Cloud 版本？

不可以。这两个版本是框架兼容性基线，Enforcer 会拦截覆盖行为。

### 业务应用可以指定第三方依赖版本吗？

可以，但应谨慎。通用依赖应优先提交到架构组，由 `microservice-framework-dependencies` 统一管理。

### 为什么 starter 不继承 `microservice-framework-starter-parent`？

`starter-parent` 是业务应用入口，会提供 Spring Boot 可执行包能力。框架 starter 是类库，应继承根 `microservice-framework-parent`，避免被错误打成 fat jar。
