package com.microservice.framework.database.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadWriteRoutingContextTest {

    @Test
    @DisplayName("普通读默认路由读库")
    void normalReadShouldRouteToReplicaByDefault() {
        ReadWriteRoutingContext context = new ReadWriteRoutingContext(true);

        assertThat(context.currentRoute()).isEqualTo(RoutingHint.READ_REPLICA);
    }

    @Test
    @DisplayName("事务内读应路由主库")
    void transactionShouldRouteToPrimary() {
        ReadWriteRoutingContext context = new ReadWriteRoutingContext(true);

        try (ReadWriteRoutingContext.Scope ignored = context.transaction()) {
            assertThat(context.currentRoute()).isEqualTo(RoutingHint.PRIMARY);
        }

        assertThat(context.currentRoute()).isEqualTo(RoutingHint.READ_REPLICA);
    }

    @Test
    @DisplayName("写后读保护开启时写入后读应路由主库")
    void readAfterWriteShouldRouteToPrimaryWhenEnabled() {
        ReadWriteRoutingContext context = new ReadWriteRoutingContext(true);

        context.markWrite();

        assertThat(context.currentRoute()).isEqualTo(RoutingHint.PRIMARY);
    }

    @Test
    @DisplayName("写后读保护关闭时写入后读可回到读库")
    void readAfterWriteCanRouteToReplicaWhenDisabled() {
        ReadWriteRoutingContext context = new ReadWriteRoutingContext(false);

        context.markWrite();

        assertThat(context.currentRoute()).isEqualTo(RoutingHint.READ_REPLICA);
    }

    @Test
    @DisplayName("显式主库提示应覆盖默认读库路由")
    void explicitPrimaryHintShouldOverrideReplicaDefault() {
        ReadWriteRoutingContext context = new ReadWriteRoutingContext(true);

        try (ReadWriteRoutingContext.Scope ignored = context.force(RoutingHint.PRIMARY)) {
            assertThat(context.currentRoute()).isEqualTo(RoutingHint.PRIMARY);
        }

        assertThat(context.currentRoute()).isEqualTo(RoutingHint.READ_REPLICA);
    }
}
