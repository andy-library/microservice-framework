
### **统一基础框架“创世纪”项目需求文档 (PRD) - v2.0 (定稿)**

Author: Andy Yang

#### **1. 背景与目标**

##### **1.1. 项目背景**
为了统一组织内部Java技术栈、提升研发效率、保障代码质量和线上稳定性，我们启动代号为“创世纪”(Genesis)的通用基础框架项目。

##### **1.2. 核心目标**
*   **依赖统一:** 提供一个权威的“物料清单”(BOM)，统一管理所有第三方及内部公共组件的版本。
*   **最佳实践:** 沉淀框架级的最佳实践，通过Starter的形式提供给业务项目，实现开箱即用。
*   **版本协同:** 建立“版本火车”发布机制，确保框架内所有组件的兼容性。
*   **架构守护:** 通过构建工具强制执行架构规范，防止架构腐化。

##### **1.3. 本文档读者**
本文档的主要读者为**AI开发大模型**及**所有框架使用方和Java研发工程师**。所有描述都应被视为精确、无歧义的开发指令。

---

#### **2. 核心架构决策与治理模型**

我们采纳一种**逻辑聚合、物理分离**的治理模型，以契合多仓库、代码隔离的现实。

*   **多仓库物理分离 (Multi-Repo):** 框架的每一个组件（如`core`, `logging-starter`, `redis-starter`）都存在于其独立的Git仓库中，由各自的团队负责维护，权限相互隔离。
*   **BOM逻辑聚合 (BOM-centric):** 框架的版本一致性与完整性，由一个核心的`microservice-framework-bom`来保证。这个BOM定义了“版本火车”包含的所有内部组件的精确版本。
*   **版本火车 (Release Train):** 所有框架内的Starter组件将跟随一个统一的主版本号同步发布。当`1.2.0`版本发布时，意味着所有包含在该版本火车里的组件都已更新并经过了集成验证。
*   **集成验证 (Integration Validation):** 发布流程的核心环节。在正式发布前，CI/CD系统必须在一个独立的、专门的测试应用仓库中，使用即将发布的BOM来构建一个包含所有Starter的测试项目，并运行框架级的集成测试套件，以确保所有组件协同工作正常。
*   **单一版本号来源:** 使用根父POM中的`<revision>`属性配合`flatten-maven-plugin`，实现框架内部所有模块版本号的单点维护。CI/CD在发布时通过`-Drevision=...`命令注入版本号。

---

#### **3. 框架的四大核心支柱 (The Four Pillars)**

框架由四个核心的`pom`类型模块构成，它们共同组成了整个框架的骨架。每个模块都拥有严格定义的职责边界。

#### **3.1. 构建父POM: `microservice-framework-parent` (The Builder)**

这是整个框架的**“构建宪法”**，是所有**框架内部组件**（Starters）构建行为的唯一权威。

*   **核心职责与架构原则:**
    1.  **全局构建环境定义:** 通过`<pluginManagement>`统一管理所有核心Maven插件的版本和基础配置。
    2.  **版本“单点真理”:** 作为“版本火车”的起点，在`<properties>`中定义`revision`、`java.version`等顶层全局版本号。
    3.  **发布流程标准化:** 通过`<distributionManagement>`定义所有框架产物的发布目的地。
    4.  **标准化发布产物:** **(新增需求)** 通过一个名为`release`的Profile，定义并标准化正式发布时的产物。当激活此Profile时，必须确保源码包（sources.jar）和JavaDoc包（javadoc.jar）被一同构建并发布到仓库，以符合开源规范和可维护性要求。
    5.  **架构守护:** 通过`maven-enforcer-plugin`等插件，强制执行Java版本、依赖收敛、禁止危险依赖等规则。
    6.  **黄金法则 #1: 严禁管理依赖。** 此POM**永远不应该**包含`<dependencyManagement>`或`<dependencies>`。
    7.  **黄金法则 #2: 严禁聚合模块。** 此POM**永远不应该**包含**未被注释掉的**`<modules>`标签。
    8.  **黄金法则 #3: 仅供内部使用。** 此POM仅服务于`microservice-framework`项目自身的组件构建。任何业务项目**绝对禁止**直接继承它。

*   **`pom.xml` 完整定义:**
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <groupId>com.microservice.framework</groupId>
        <artifactId>microservice-framework-parent</artifactId>
        <version>${revision}</version>
        <packaging>pom</packaging>

        <properties>
            <revision>1.2.0-SNAPSHOT</revision>
            <java.version>21</java.version>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            <spring-boot.version>3.2.5</spring-boot.version>
            <spring-cloud.version>2023.0.1</spring-cloud.version>
        </properties>

        <build>
            <pluginManagement>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>flatten-maven-plugin</artifactId>
                        <version>1.6.0</version>
                        <configuration>
                            <updatePomFile>true</updatePomFile>
                            <flattenMode>resolveCiFriendliesOnly</flattenMode>
                        </configuration>
                        <executions>
                            <execution>
                                <id>flatten</id>
                                <phase>process-resources</phase>
                                <goals><goal>flatten</goal></goals>
                            </execution>
                            <execution>
                                <id>flatten.clean</id>
                                <phase>clean</phase>
                                <goals><goal>clean</goal></goals>
                            </execution>
                        </executions>
                    </plugin>
                    <!-- (新增) 标准化源码和Javadoc插件 -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-source-plugin</artifactId>
                        <version>3.3.0</version>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-javadoc-plugin</artifactId>
                        <version>3.6.3</version>
                    </plugin>
                    <!-- 其他插件定义... -->
                </plugins>
            </pluginManagement>
        </build>

        <!-- (新增) 标准化发布Profile -->
        <profiles>
            <profile>
                <id>release</id>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-source-plugin</artifactId>
                            <executions>
                                <execution>
                                    <id>attach-sources</id>
                                    <goals>
                                        <goal>jar-no-fork</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-javadoc-plugin</artifactId>
                            <executions>
                                <execution>
                                    <id>attach-javadocs</id>
                                    <goals>
                                        <goal>jar</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
            </profile>
        </profiles>

        <!--
        ========================================================================
        == 本地开发专用 - 严禁在GIT中解除注释                                  ==
        ========================================================================
        == 这个 <modules> 区域仅为提升框架贡献者的本地开发体验而设。          ==
        == 解除注释后，你可以在此根目录通过一条命令 (如 `mvn clean install`)    ==
        == 构建所有核心框架模块，便于进行跨模块的本地联调。                   ==
        ==                                                                    ==
        == 警告：此区域在提交到版本控制系统时，必须保持注释状态。               ==
        == 官方的构建与发布流程由CI/CD系统管理，不依赖此静态聚合。              ==
        == 提交一个激活的 <modules> 标签将严重破坏本架构的“多仓库分离”原则。   ==
        ========================================================================
        -->
        <!--
        <modules>
            <module>../microservice-framework-dependencies</module>
            <module>../microservice-framework-bom</module>
            <module>../microservice-framework-starter-parent</module>
        </modules>
        -->
    </project>
    ```

#### **3.2. 依赖定义POM: `microservice-framework-dependencies` (The Librarian)**

这是框架的**“外部依赖图书馆”**，负责统一管理所有**第三方**开源组件的版本。

*   **核心职责与架构原则:**
    1.  **第三方依赖的唯一管理者:** 其核心职责是在`<dependencyManagement>`中导入`spring-boot-dependencies`和`spring-cloud-dependencies`，并可覆盖或补充其他必要的第三方库版本。
    2.  **继承构建宪法:** 必须继承`microservice-framework-parent`以确保构建行为和版本号的一致性。
    3.  **黄金法则 #1: 严禁定义内部依赖。** 此POM**永远不应该**管理`microservice-framework-*-starter`的版本。它的职责是**管理外部世界**。
    4.  **黄金法则 #2: 仅供内部聚合。** 此POM不应被业务项目直接使用，它将被`starter-parent`所导入。

*   **`pom.xml` 完整定义:**
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <parent>
            <groupId>com.microservice.framework</groupId>
            <artifactId>microservice-framework-parent</artifactId>
            <version>${revision}</version>
            <relativePath/>
        </parent>

        <artifactId>microservice-framework-dependencies</artifactId>
        <packaging>pom</packaging>

        <name>Microservice Framework Dependencies</name>
        <description>The single source of truth for all third-party dependency versions.</description>

        <dependencyManagement>
            <dependencies>
                <!-- 1. 导入Spring Boot的权威BOM -->
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-dependencies</artifactId>
                    <version>${spring-boot.version}</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
                <!-- 2. 导入Spring Cloud的权威BOM -->
                <dependency>
                    <groupId>org.springframework.cloud</groupId>
                    <artifactId>spring-cloud-dependencies</artifactId>
                    <version>${spring-cloud.version}</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
                <!-- 3. (可选) 在此覆盖或添加其他第三方库的版本 -->
                <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.0.0-jre</version>
                </dependency>
            </dependencies>
        </dependencyManagement>
    </project>
    ```

#### **3.3. 框架物料清单: `microservice-framework-bom` (The Release Manifest)**

这是框架的**“版本火车时刻表”**，负责声明本次发布列车中包含的所有**内部**Starter组件。

*   **核心职责与架构原则:**
    1.  **内部组件的唯一清单:** 其核心职责是在`<dependencyManagement>`中定义所有`microservice-framework-*-starter`的版本。
    2.  **版本号联动:** 所有在此定义的内部组件，其版本号必须是`${project.version}`，该版本号继承自`parent`的`${revision}`，从而确保所有组件版本与火车版本严格一致。
    3.  **继承构建宪法:** 必须继承`microservice-framework-parent`。
    4.  **黄金法则 #1: 严禁定义外部依赖。** 此POM**永远不应该**管理第三方库的版本。那是`dependencies`模块的职责。

*   **`pom.xml` 完整定义:**
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <parent>
            <groupId>com.microservice.framework</groupId>
            <artifactId>microservice-framework-parent</artifactId>
            <version>${revision}</version>
            <relativePath/>
        </parent>

        <artifactId>microservice-framework-bom</artifactId>
        <packaging>pom</packaging>

        <name>Microservice Framework BOM</name>
        <description>Bill of Materials (BOM) for all framework framework modules.</description>

        <dependencyManagement>
            <dependencies>
                <!-- 
                    定义所有属于本次版本火车的内部Starter模块。
                    版本号必须是 ${project.version}，以确保同步。
                -->
                <dependency>
                    <groupId>com.microservice.framework</groupId>
                    <artifactId>microservice-framework-logging-starter</artifactId>
                    <version>${project.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.microservice.framework</groupId>
                    <artifactId>microservice-framework-redis-starter</artifactId>
                    <version>${project.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.microservice.framework</groupId>
                    <artifactId>microservice-framework-web-starter</artifactId>
                    <version>${project.version}</version>
                </dependency>
                <!-- ... 更多内部starter ... -->
            </dependencies>
        </dependencyManagement>
    </project>
    ```

#### **3.4. 业务应用父POM: `microservice-framework-starter-parent` (The Application Entrypoint)**

这是**所有业务项目**接入框架技术体系的**唯一入口**。

*   **核心职责与架构原则:**
    1.  **业务项目的唯一父级:** 所有Spring Boot应用都必须且只能继承此POM。
    2.  **聚合依赖视图:** 通过`import`方式，同时导入`microservice-framework-dependencies`和`microservice-framework-bom`，为业务项目提供一个包含了内、外部所有依赖的完整、一致的依赖视图。
    3.  **提供应用级构建支持:** 在`<pluginManagement>`中为业务项目预配置好`spring-boot-maven-plugin`等应用打包、运行所必需的插件。
    4.  **继承构建宪法:** 必须继承`microservice-framework-parent`以加入统一版本管理体系。
    5.  **黄金法则 #1: 隐藏底层复杂性。** 业务开发者只需继承此parent，即可获得所有好处，无需关心其他三个核心POM的存在。

*   **`pom.xml` 完整定义:**
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <parent>
            <groupId>com.microservice.framework</groupId>
            <artifactId>microservice-framework-parent</artifactId>
            <version>${revision}</version>
            <relativePath/>
        </parent>

        <artifactId>microservice-framework-starter-parent</artifactId>
        <packaging>pom</packaging>

        <name>Microservice Framework Starter Parent</name>
        <description>The single parent POM for all business applications.</description>

        <dependencyManagement>
            <dependencies>
                <!-- 1. 导入外部依赖库 -->
                <dependency>
                    <groupId>com.microservice.framework</groupId>
                    <artifactId>microservice-framework-dependencies</artifactId>
                    <version>${project.version}</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
                <!-- 2. 导入内部模块清单 -->
                <dependency>
                    <groupId>com.microservice.framework</groupId>
                    <artifactId>microservice-framework-bom</artifactId>
                    <version>${project.version}</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
        
        <build>
            <pluginManagement>
                <plugins>
                    <!-- 为业务项目提供可执行jar包的打包能力 -->
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <version>${spring-boot.version}</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>repackage</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </pluginManagement>
        </build>
    </project>
    ```
