package com.microservice.demo;

import com.microservice.demo.controller.AuditDemoController;
import com.microservice.demo.controller.CommonDemoController;
import com.microservice.demo.controller.DroolsDemoController;
import com.microservice.demo.controller.EncryptionDemoController;
import com.microservice.demo.controller.JsonDemoController;
import com.microservice.demo.controller.StorageDemoController;
import com.microservice.demo.controller.RedisDemoController;
import com.microservice.demo.controller.KafkaDemoController;
import com.microservice.demo.controller.ElasticsearchDemoController;
import com.microservice.demo.controller.AsyncDemoController;
import com.microservice.demo.controller.JobDemoController;
import com.microservice.demo.controller.DatabaseDemoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Starter capability controllers require starter API beans")
class StarterControllerDependencyTest {

    @Test
    @DisplayName("common controller fails to start without common starter beans")
    void commonControllerRequiresStarterBeans() {
        assertControllerFailsWithoutDependencies(CommonDemoController.class);
    }

    @Test
    @DisplayName("json controller fails to start without JsonCodec")
    void jsonControllerRequiresStarterBean() {
        assertControllerFailsWithoutDependencies(JsonDemoController.class);
    }

    @Test
    @DisplayName("audit controller fails to start without AuditRecorder")
    void auditControllerRequiresStarterBean() {
        assertControllerFailsWithoutDependencies(AuditDemoController.class);
    }

    @Test
    @DisplayName("field encryption controller fails to start without FieldEncryptor")
    void encryptionControllerRequiresStarterBean() {
        assertControllerFailsWithoutDependencies(EncryptionDemoController.class);
    }

    @Test
    @DisplayName("object storage controller fails to start without ObjectStorageOperations")
    void storageControllerRequiresStarterBeans() {
        assertControllerFailsWithoutDependencies(StorageDemoController.class);
    }

    @Test
    @DisplayName("drools controller fails to start without RuleEngine")
    void droolsControllerRequiresStarterBeans() {
        assertControllerFailsWithoutDependencies(DroolsDemoController.class);
    }

    @Test void redisControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(RedisDemoController.class); }
    @Test void kafkaControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(KafkaDemoController.class); }
    @Test void elasticsearchControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(ElasticsearchDemoController.class); }
    @Test void asyncControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(AsyncDemoController.class); }
    @Test void jobControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(JobDemoController.class); }
    @Test void databaseControllerRequiresStarterBeans() { assertControllerFailsWithoutDependencies(DatabaseDemoController.class); }

    private void assertControllerFailsWithoutDependencies(Class<?> controllerClass) {
        new ApplicationContextRunner()
                .withBean(controllerClass)
                .run(context -> assertThat(context).hasFailed());
    }
}
