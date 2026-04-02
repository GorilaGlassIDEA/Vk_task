package by.dima.input;

import io.tarantool.client.TarantoolClient;
import io.tarantool.mapping.TarantoolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TarantoolRepository {

    private final TarantoolClient client;
    private final static String SPACE_NAME = "KV";


    public long count() {
        try {
            String lua = String.format("return box.space['%s']:count()", SPACE_NAME);
            CompletableFuture<TarantoolResponse<List<?>>> future = client.eval(lua);
            List<?> result = future.get().get();

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
            String lua = String.format("box.space['%s']:replace({...})", SPACE_NAME);
            client.eval(lua, List.of(key, value)).get();
            log.debug("Put key: {}", key);
        } catch (Exception e) {
            log.error("Failed to put key {}", key);
            throw new RuntimeException("Put failed", e);
        }

    }

    public Optional<byte[]> get(String key) {

        try {
            String lua = String.format("local val = box.space['%s']:get({...}) return val and val[2] or nil", SPACE_NAME);
            List<?> result = (List<?>) client.eval(lua, List.of(key)).get().get();

            if (result != null && !result.isEmpty() && result.get(0) != null) {
                Object value = result.get(0);
                if (value instanceof byte[]) {
                    return Optional.of((byte[]) value);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get key: {}", key, e);
            return Optional.empty();
        }
    }

    public boolean delete(String key) {
        try {
            String lua = "return box.space.KV:delete({...}) ~= nil";

            List<?> result = (List<?>) client.eval(lua, List.of(key)).get().get();

            boolean deleted = result != null && !result.isEmpty() && result.get(0) != null;

            log.debug("Delete key={} success={}", key, deleted);
            return deleted;

        } catch (Exception e) {
            log.error("Failed to delete key: {}", key, e);
            throw new RuntimeException("Delete failed for key: " + key, e);
        }
    }

    public List<KeyValuePair> range(String keyStart, String keyEnd) {
        try {
            String lua = String.format(
                    "local space = box.space['%s'] " +
                    "local result = {} " +
                    "for _, tuple in space:pairs({%s},{iterator='GE'}) do " +
                    "   if tuple[1]> '%s' then break end " +
                    "   table.insert(result,tuple)" +
                    "end " +
                    "return result", SPACE_NAME, keyStart, keyEnd);
            List<?> rawResult = (List<?>) client.eval(lua).get().get();
            return rawResult.stream()
                    .map(item -> {
                        if (!(item instanceof List<?> tuple)) {
                            throw new IllegalStateException("Unexpected item type: " + item.getClass());
                        }
                        Object keyObj = tuple.get(0);
                        Object valueObj = tuple.size() > 1 ? tuple.get(1) : null;

                        String k = keyObj != null ? keyObj.toString() : "";

                        byte[] v = null;
                        if (valueObj instanceof byte[]) {
                            v = (byte[]) valueObj;
                        } else if (valueObj != null) {
                            v = valueObj.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        }

                        return new KeyValuePair(k, v);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get range from {} to {}", keyStart, keyEnd, e);
            throw new RuntimeException("Range query failed", e);
        }
    }
    public void truncate() {
        // !! For tests !!
        try {
            client.eval("box.space.KV:truncate()").get();
            log.info("Space KV truncated");
        } catch (Exception e) {
            log.error("Failed to truncate", e);
        }
    }
    public record KeyValuePair(String key, byte[] value) {
    }

}
