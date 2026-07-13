package com.microservice.framework.drools.autoconfigure;

import com.microservice.framework.drools.DroolsProperties;
import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleExecutionResult;
import com.microservice.framework.drools.api.RuleSession;
import com.microservice.framework.drools.api.RuleVersion;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Drools Starter 自动配置
 * <p>
 * 根据 {@code framework.drools.rule.enabled} 属性决定是否激活，默认启用。
 * 注册以下 Bean：
 * - {@link RuleEngine}：规则引擎核心接口
 * - {@link RuleVersion}：当前规则包版本信息
 * <p>
 * 当 {@code framework.drools.session.enabled=true}（默认）时，
 * 还注册 {@link RuleSession} 的创建能力。
 * <p>
 * 当 Drools 运行时（drools-core）不存在时，注册基于内存的默认实现，
 * 仅提供接口骨架，不执行实际规则逻辑。

 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(DroolsProperties.class)
@ConditionalOnProperty(prefix = "framework.drools.rule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DroolsAutoConfiguration {

    /**
     * 注册 RuleEngine Bean
     * <p>
     * 当容器中不存在 RuleEngine 时，注册基于内存的默认实现。
     * 默认实现仅提供接口骨架，实际规则执行需要 Drools 运行时依赖。
     *
     * @param properties Drools 配置属性
     * @return RuleEngine 实例
     */
    @Bean
    @ConditionalOnMissingBean(RuleEngine.class)
    public RuleEngine ruleEngine(DroolsProperties properties) {
        assertDroolsRuntimePresent();
        return new DefaultRuleEngine(properties);
    }

    /**
     * 注册 RuleVersion Bean
     * <p>
     * 提供当前规则包的版本信息，用于运行治理和版本追踪。
     *
     * @param properties Drools 配置属性
     * @return RuleVersion 实例
     */
    @Bean
    @ConditionalOnMissingBean(RuleVersion.class)
    public RuleVersion ruleVersion(DroolsProperties properties) {
        return new RuleVersion(
                "com.microservice.framework",
                "drools-rules",
                "1.0.0-alpha.1",
                properties.getRule().getRuleFiles().size());
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    private void assertDroolsRuntimePresent() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (!ClassUtils.isPresent("org.kie.internal.utils.KieHelper", classLoader)
                || !ClassUtils.isPresent("org.kie.api.runtime.KieSession", classLoader)
                || !ClassUtils.isPresent("org.drools.mvel.MVELConstraint", classLoader)) {
            throw new IllegalStateException("Drools rule engine is enabled but Drools runtime/compiler dependencies are not available");
        }
    }

    /**
     * 基于 API 的默认 RuleEngine 实现
     * <p>
     * 从配置的规则文件加载并编译 DRL，提供无状态执行、规则校验和
     * 基于规则元数据的分组查询能力。
     */
    static class DefaultRuleEngine implements RuleEngine {

        private final DroolsProperties properties;
        private final KieBase kieBase;
        private final List<String> compilationErrors;
        private final List<String> ruleNames;
        private final Map<String, List<String>> ruleGroups;

        DefaultRuleEngine(DroolsProperties properties) {
            this.properties = properties;
            if (properties.getRule().getRuleFiles().isEmpty()) {
                throw new IllegalStateException("Drools rule engine is enabled but no rule files are configured");
            }

            CompilationResult compilationResult = compileRules(properties.getRule().getRuleFiles());
            this.kieBase = compilationResult.kieBase();
            this.compilationErrors = compilationResult.errors();
            this.ruleNames = compilationResult.ruleNames();
            this.ruleGroups = compilationResult.ruleGroups();

            if (Boolean.TRUE.equals(properties.getRule().getValidateOnStartup()) && !compilationErrors.isEmpty()) {
                throw compilationFailure("Drools rule compilation failed");
            }
        }

        @Override
        public RuleExecutionResult execute(Collection<?> facts) {
            return executeWithFacts(facts, Collections.emptyMap());
        }

        @Override
        public RuleExecutionResult executeWithFacts(Collection<?> facts, Map<String, Object> globals) {
            assertValidRuleBase();
            long startedAt = System.nanoTime();
            List<Object> outputFacts = new ArrayList<>();
            List<String> matchedRules = new ArrayList<>();
            List<String> firedRules = new ArrayList<>();
            KieSession session = kieBase.newKieSession();
            try {
                session.addEventListener(new DefaultAgendaEventListener() {
                    @Override
                    public void matchCreated(MatchCreatedEvent event) {
                        matchedRules.add(event.getMatch().getRule().getName());
                    }

                    @Override
                    public void afterMatchFired(AfterMatchFiredEvent event) {
                        firedRules.add(event.getMatch().getRule().getName());
                    }
                });
                for (Map.Entry<String, Object> entry : safeGlobals(globals).entrySet()) {
                    session.setGlobal(entry.getKey(), entry.getValue());
                }
                for (Object fact : safeFacts(facts)) {
                    session.insert(fact);
                    outputFacts.add(fact);
                }
                session.fireAllRules();
                long executionTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
                return new RuleExecutionResult(matchedRules, firedRules, outputFacts, executionTimeMs);
            } finally {
                session.dispose();
            }
        }

        @Override
        public boolean validate() {
            return compilationErrors.isEmpty();
        }

        @Override
        public int getRuleCount() {
            return ruleNames.size();
        }

        @Override
        public List<String> getRuleGroup(String group) {
            return ruleGroups.getOrDefault(group, Collections.emptyList());
        }

        private void assertValidRuleBase() {
            if (!compilationErrors.isEmpty() || kieBase == null) {
                throw new IllegalStateException("Drools rule compilation failed: " + String.join(System.lineSeparator(), compilationErrors));
            }
        }

        private IllegalStateException compilationFailure(String message) {
            IllegalStateException exception = new IllegalStateException(message);
            if (!compilationErrors.isEmpty()) {
                exception.addSuppressed(new IllegalStateException(String.join(System.lineSeparator(), compilationErrors)));
            }
            return exception;
        }

        private CompilationResult compileRules(List<String> ruleFiles) {
            KieHelper helper = new KieHelper();
            for (String ruleFile : ruleFiles) {
                helper.addResource(resourceFor(ruleFile), ResourceType.DRL);
            }

            Results results = helper.verify();
            List<String> errors = results.getMessages(Message.Level.ERROR).stream()
                    .map(Message::toString)
                    .collect(Collectors.toList());
            if (!errors.isEmpty()) {
                return CompilationResult.invalid(errors);
            }

            KieBase compiledKieBase = helper.build();
            List<Rule> compiledRules = compiledKieBase.getKiePackages().stream()
                    .map(KiePackage::getRules)
                    .flatMap(Collection::stream)
                    .sorted((left, right) -> left.getName().compareTo(right.getName()))
                    .collect(Collectors.toList());
            List<String> compiledRuleNames = compiledRules.stream()
                    .map(Rule::getName)
                    .collect(Collectors.toUnmodifiableList());
            Map<String, List<String>> compiledRuleGroups = groupsFor(compiledRules);
            return new CompilationResult(compiledKieBase, Collections.emptyList(), compiledRuleNames, compiledRuleGroups);
        }

        private Resource resourceFor(String ruleFile) {
            String location = Objects.requireNonNull(ruleFile, "ruleFile must not be null").trim();
            if (location.isEmpty()) {
                throw new IllegalStateException("Drools rule file location must not be blank");
            }
            if (location.startsWith("classpath:")) {
                return ResourceFactory.newClassPathResource(location.substring("classpath:".length()));
            }
            if (location.startsWith("file:")) {
                return ResourceFactory.newFileResource(location.substring("file:".length()));
            }
            java.io.File file = new java.io.File(location);
            if (file.exists()) {
                return ResourceFactory.newFileResource(file);
            }
            return ResourceFactory.newClassPathResource(location);
        }

        private Map<String, List<String>> groupsFor(List<Rule> rules) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (Rule rule : rules) {
                Object group = rule.getMetaData().get("group");
                if (group != null) {
                    groups.computeIfAbsent(group.toString(), ignored -> new ArrayList<>()).add(rule.getName());
                }
            }
            Map<String, List<String>> immutableGroups = new LinkedHashMap<>();
            Set<String> configuredGroups = new TreeSet<>(properties.getRule().getGroups());
            configuredGroups.addAll(groups.keySet());
            for (String group : configuredGroups) {
                immutableGroups.put(group, Collections.unmodifiableList(new ArrayList<>(groups.getOrDefault(group, Collections.emptyList()))));
            }
            return Collections.unmodifiableMap(immutableGroups);
        }

        private Collection<?> safeFacts(Collection<?> facts) {
            return facts == null ? Collections.emptyList() : facts;
        }

        private Map<String, Object> safeGlobals(Map<String, Object> globals) {
            return globals == null ? Collections.emptyMap() : globals;
        }

        private record CompilationResult(KieBase kieBase, List<String> errors, List<String> ruleNames,
                                         Map<String, List<String>> ruleGroups) {

            private static CompilationResult invalid(List<String> errors) {
                return new CompilationResult(null, Collections.unmodifiableList(new ArrayList<>(errors)),
                        Collections.emptyList(), Collections.emptyMap());
            }
        }
    }
}
