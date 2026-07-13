package com.microservice.demo.controller;

import com.microservice.framework.apollo.config.SensitiveConfigMasker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * nacos-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/nacos")
public class NacosTestController extends ConfigDemoController {

    public NacosTestController(ApplicationContext context,
                               Environment environment,
                               ObjectProvider<com.microservice.framework.nacos.config.SensitiveConfigMasker> nacosMasker,
                               ObjectProvider<SensitiveConfigMasker> apolloMasker) {
        super(context, environment, nacosMasker, apolloMasker);
    }
}
