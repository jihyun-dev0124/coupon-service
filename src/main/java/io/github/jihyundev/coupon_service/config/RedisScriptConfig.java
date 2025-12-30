package io.github.jihyundev.coupon_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    /**
     * gate.lua return codes:
     *  1: OK (stock decremented + user marked as issued)
     *  0: SOLD_OUT
     *  2: ALREADY_ISSUED
     * @return
     */
    @Bean
    public DefaultRedisScript<Long> couponGateScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/gate.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<List> zsetClaimScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/zclaim.lua")));
        script.setResultType(List.class);
        return script;
    }
}
