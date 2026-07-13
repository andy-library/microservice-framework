package com.microservice.demo.controller;

import com.microservice.framework.nacos.config.SensitiveConfigMasker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * apollo-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/apollo")
public class ApolloTestController extends ConfigDemoController {

    public ApolloTestController(ApplicationContext context,
                                Environment environment,
                                ObjectProvider<SensitiveConfigMasker> nacosMasker,
                                ObjectProvider<com.microservice.framework.apollo.config.SensitiveConfigMasker> apolloMasker) {
        super(context, environment, nacosMasker, apolloMasker);
    }
}
