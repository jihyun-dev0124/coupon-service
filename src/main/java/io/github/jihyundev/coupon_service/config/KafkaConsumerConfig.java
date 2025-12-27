package io.github.jihyundev.coupon_service.config;

import io.github.jihyundev.coupon_service.messaging.CouponIssueRequested;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, CouponIssueRequested> consumerFactory(Environment env) {
        JsonDeserializer<CouponIssueRequested> valueDeserializer = new JsonDeserializer<>(CouponIssueRequested.class);
        valueDeserializer.addTrustedPackages("io.github.jihyundev.coupon_service.messaging");
        valueDeserializer.setUseTypeHeaders(false);

        //application.yml 설정 값 주입
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("app.kafka.consumer-group", "coupon-issue-consumer"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); //소비자그룹의 커밋된 오프셋 없으면 처음부터 읽어옴.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); //자동 오프셋 커밋 끔, 메시지 처리 성공했을때 수동으로 커밋할거임. - 클라이언트 level

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequested> kafkaListenerContainerFactory(ConsumerFactory<String, CouponIssueRequested> cf) {
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueRequested> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(3); //같은 리스너 동시에 3개 스레드(컨테이너)로 실행
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);//Ack(오프셋 커밋) 모드를 수동(Manual)으로 설정함. -> DB 반영/쿠폰 발급 성공 확인 후 수동 커밋 - container level

        //실패 시 3회 재시도 후 DLQ 없이 포기(샘플). 실무에서는 DLQ 토픽 권장
        DefaultErrorHandler eh = new DefaultErrorHandler(new FixedBackOff(1000L, 3));//실패하면 1초 간격으로 3번 더 재시도
        factory.setCommonErrorHandler(eh);
        return factory;
    }
}
