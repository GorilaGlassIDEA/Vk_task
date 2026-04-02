package by.dima.input;

import io.tarantool.client.TarantoolClient;
import io.tarantool.mapping.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TarantoolRepository {

    private final TarantoolClient client;
    private final static String SPACE_NAME = "KV";


    public long count() {
        try {
            String lua = String.format("return box.space['%s']:count()", SPACE_NAME);
            List<?> result = (List<?>) client.eval(lua);

            if (result != null && !result.isEmpty() && result.get(0) instanceof Number) {
                return ((Number) result.get(0)).longValue();
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to count records", e);
            return 0;
        }
    }

    public void put(String key, byte[] value) {
        try {
            List<Object> values = List.of(key, value);
            Tuple<List<Object>> tuple = new Tuple<>(values, null);

            client.space(SPACE_NAME).replaceObject(List.of(key, value)).get();
            log.debug("Put key: {}", key);
        } catch (Exception e) {
            log.error("Failed to put key {}", key);
            throw new RuntimeException("Put failed", e);
        }

    }
}
