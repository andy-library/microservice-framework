package com.microservice.demo.controller;

import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleExecutionResult;
import com.microservice.framework.drools.api.RuleVersion;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * drools-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/drools")
public class DroolsDemoController {

    private final RuleEngine ruleEngine;
    private final RuleVersion ruleVersion;

    public DroolsDemoController(RuleEngine ruleEngine, RuleVersion ruleVersion) {
        this.ruleEngine = ruleEngine;
        this.ruleVersion = ruleVersion;
    }

    @PostMapping({"/execute", "/member-level"})
    public ApiResponse<Map<String, Object>> execute(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> fact = body == null ? Map.of() : body;
        RuleExecutionResult result = ruleEngine.execute(List.of(fact));
        String memberLevel = result.getOutputFacts().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(output -> output.get("memberLevel"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .findFirst()
                .orElse("UNKNOWN");
        return ApiResponse.success(Map.of("matched", result.hasMatchedRules(), "engineAvailable", ruleEngine.validate(),
                "memberLevel", memberLevel, "input", fact, "firedRules", result.getFiredRules(),
                "matchedRules", result.getMatchedRules(), "executionTimeMs", result.getExecutionTimeMs()));
    }

    @GetMapping("/rule-version")
    public ApiResponse<Map<String, Object>> version() {
        return ApiResponse.success(Map.of("coordinates", ruleVersion.getCoordinates(),
                "ruleCount", ruleVersion.getRuleCount(), "engineValidated", ruleEngine.validate()));
    }
}
