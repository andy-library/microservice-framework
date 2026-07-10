package com.microservice.demo.embedded;

import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleExecutionResult;
import com.microservice.framework.drools.api.RuleVersion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded Drools Configuration
 *
 * Provides a {@link SimpleRuleEngine} bean that actually evaluates rules (not stubs)
 * and a {@link RuleVersion} bean with embedded metadata. Overrides the starter's
 * default implementations via {@code @ConditionalOnMissingBean}. Activates only when
 * {@code framework.drools.provider=embedded} is set.
 *
 * <p>The SimpleRuleEngine evaluates member-level rules based on spending amount thresholds
 * defined in globals, matching the demo controller's usage pattern. This is a genuine
 * rule engine that works without the Drools runtime.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.drools", name = "provider", havingValue = "embedded")
public class EmbeddedDroolsConfiguration {

    // ======================================================================
    // RuleEngine
    // ======================================================================

    @Bean
    public RuleEngine simpleRuleEngine() {
        return new SimpleRuleEngine();
    }

    // ======================================================================
    // RuleVersion
    // ======================================================================

    @Bean
    public RuleVersion embeddedRuleVersion() {
        return new RuleVersion(
                "com.microservice.framework",
                "embedded-rules",
                "1.0.0-EMBEDDED",
                4 // DIAMOND, GOLD, SILVER, BRONZE rules
        );
    }

    /**
     * A genuine rule engine that evaluates member-level rules without requiring
     * the Drools runtime. Matches the demo controller's expected usage pattern:
     * facts contain a "spendingAmount" field (or "amount" as fallback), and globals
     * provide threshold values (thresholdDiamond, thresholdGold, thresholdSilver).
     *
     * <p>Rule evaluation logic:</p>
     * <ul>
     *   <li>If spendingAmount >= thresholdDiamond → DIAMOND</li>
     *   <li>If spendingAmount >= thresholdGold → GOLD</li>
     *   <li>If spendingAmount >= thresholdSilver → SILVER</li>
     *   <li>Otherwise → BRONZE</li>
     * </ul>
     */
    static class SimpleRuleEngine implements RuleEngine {

        /** Built-in rule definitions for member level evaluation. */
        private static final List<RuleDefinition> BUILTIN_RULES = List.of(
                new RuleDefinition("member-level-diamond", "membership",
                        "spendingAmount >= thresholdDiamond → DIAMOND"),
                new RuleDefinition("member-level-gold", "membership",
                        "spendingAmount >= thresholdGold → GOLD"),
                new RuleDefinition("member-level-silver", "membership",
                        "spendingAmount >= thresholdSilver → SILVER"),
                new RuleDefinition("member-level-bronze", "membership",
                        "spendingAmount < thresholdSilver → BRONZE")
        );

        @Override
        public RuleExecutionResult execute(Collection<?> facts) {
            // Use default thresholds when no globals are provided
            Map<String, Object> defaultGlobals = new LinkedHashMap<>();
            defaultGlobals.put("thresholdDiamond", 10000);
            defaultGlobals.put("thresholdGold", 5000);
            defaultGlobals.put("thresholdSilver", 1000);
            return executeWithFacts(facts, defaultGlobals);
        }

        @Override
        public RuleExecutionResult executeWithFacts(Collection<?> facts,
                                                    Map<String, Object> globals) {
            long startTime = System.currentTimeMillis();

            if (facts == null || facts.isEmpty()) {
                return RuleExecutionResult.empty(System.currentTimeMillis() - startTime);
            }

            // Extract thresholds from globals
            double thresholdDiamond = getDoubleValue(globals, "thresholdDiamond", 10000);
            double thresholdGold = getDoubleValue(globals, "thresholdGold", 5000);
            double thresholdSilver = getDoubleValue(globals, "thresholdSilver", 1000);

            List<String> matchedRules = new ArrayList<>();
            List<String> firedRules = new ArrayList<>();
            List<Object> outputFacts = new ArrayList<>();

            for (Object fact : facts) {
                if (fact instanceof Map<?, ?> factMap) {
                    // Extract spending amount (support both "spendingAmount" and "amount" field names)
                    double amount = extractAmount(factMap);

                    // Evaluate each rule against the fact
                    String memberLevel;
                    if (amount >= thresholdDiamond) {
                        matchedRules.add("member-level-diamond");
                        firedRules.add("member-level-diamond");
                        memberLevel = "DIAMOND";
                    } else if (amount >= thresholdGold) {
                        matchedRules.add("member-level-gold");
                        firedRules.add("member-level-gold");
                        memberLevel = "GOLD";
                    } else if (amount >= thresholdSilver) {
                        matchedRules.add("member-level-silver");
                        firedRules.add("member-level-silver");
                        memberLevel = "SILVER";
                    } else {
                        matchedRules.add("member-level-bronze");
                        firedRules.add("member-level-bronze");
                        memberLevel = "BRONZE";
                    }

                    // Build output fact with the evaluated member level
                    Map<String, Object> outputFact = new LinkedHashMap<>();
                    outputFact.put("type", "MemberEvaluation");
                    outputFact.put("spendingAmount", amount);
                    outputFact.put("memberLevel", memberLevel);
                    outputFact.put("evaluatedBy", "SimpleRuleEngine");
                    outputFacts.add(outputFact);
                } else {
                    // Non-Map facts are passed through as-is
                    outputFacts.add(fact);
                }
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            return new RuleExecutionResult(matchedRules, firedRules, outputFacts, executionTimeMs);
        }

        @Override
        public boolean validate() {
            // In-memory rules are always valid
            return true;
        }

        @Override
        public int getRuleCount() {
            return BUILTIN_RULES.size();
        }

        @Override
        public List<String> getRuleGroup(String group) {
            if (group == null) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>();
            for (RuleDefinition rule : BUILTIN_RULES) {
                if (rule.group.equals(group)) {
                    result.add(rule.name);
                }
            }
            return result;
        }

        // ======================================================================
        // Helper methods
        // ======================================================================

        private double extractAmount(Map<?, ?> factMap) {
            Object amountObj = factMap.get("spendingAmount");
            if (amountObj == null) {
                amountObj = factMap.get("amount"); // Fallback field name
            }
            if (amountObj == null) {
                return 0.0;
            }
            return toDouble(amountObj);
        }

        private double getDoubleValue(Map<String, Object> globals, String key, double defaultValue) {
            if (globals == null) {
                return defaultValue;
            }
            Object value = globals.get(key);
            if (value == null) {
                return defaultValue;
            }
            return toDouble(value);
        }

        private double toDouble(Object value) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            return 0.0;
        }

        /** Simple rule definition record for built-in rules. */
        static class RuleDefinition {
            final String name;
            final String group;
            final String description;

            RuleDefinition(String name, String group, String description) {
                this.name = name;
                this.group = group;
                this.description = description;
            }
        }
    }
}
