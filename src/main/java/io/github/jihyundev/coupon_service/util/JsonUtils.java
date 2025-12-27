package io.github.jihyundev.coupon_service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {
    private final ObjectMapper om;
    public JsonUtils() {
        this.om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
    }

    public String toJson(Object obj) {
        try{
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    public <T> T fromJson(String s, Class<T> type) {
        try{
            return om.readValue(s, type);
        } catch (Exception e) {
            throw new IllegalStateException("JSON deserialize failed", e);
        }
    }
}
