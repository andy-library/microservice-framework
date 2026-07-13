package com.microservice.demo.controller;

import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * drools-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/drools")
public class DroolsTestController extends DroolsDemoController {

    public DroolsTestController(RuleEngine ruleEngine, RuleVersion ruleVersion) {
        super(ruleEngine, ruleVersion);
    }
}
