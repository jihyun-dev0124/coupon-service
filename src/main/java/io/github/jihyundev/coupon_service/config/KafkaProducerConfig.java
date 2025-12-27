package io.github.jihyundev.coupon_service.config;

import io.github.jihyundev.coupon_service.messaging.CouponIssueRequested;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, CouponIssueRequested> producerFactory(Environment env) {
        //application.yml 설정 값 주입
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ProducerConfig.ACKS_CONFIG, "all"); //메시지 전송 성공 판단 기준,
                                                     // all: 리더+ISR(in-sync replica) 복제본까지 모두 기록 확인을 받아야 성공 처리, 안정성은 높지만 지연은 상대적으로 증가
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); //네트워크 재시도 등으로 인해 동일 레코드 중복 전송되는 상황에서 브로커가 중복을 방지하도록 함.
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); //재시도 횟수 무한으로 설정, 타임아웃과 같이 설계해야함!
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000); //타임아웃 2분 (일반 이벤트:1분, 쿠폰/결제 등 중요 이벤트:2분, 배치성 이벤트:5분)

        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000); //브로커 한 번 호출에 응답 기다리는 최대 시간 -> 30초 넘으면 실패로 간주되고 retry 대상이 됨
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); //Kafka 레코드 key를 문자열로 직렬화
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); //Kafka 레코드 value를 JSON으로 직렬화

        // JsonSerializer: type headers 제거 (언어/서비스 간 상호운용 시 권장)
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, CouponIssueRequested> kafkaTemplate(ProducerFactory<String, CouponIssueRequested> pf) {
        return new KafkaTemplate<>(pf);
    }
}
