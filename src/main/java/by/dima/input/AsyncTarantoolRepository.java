package by.dima.input;

import io.tarantool.client.TarantoolClient;
import io.tarantool.mapping.TarantoolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AsyncTarantoolRepository {

    private final TarantoolClient client;
    private final static String SPACE_NAME = "KV";


    public CompletableFuture<Long> countAsync() {
        String lua = String.format("return box.space['%s']:count()", SPACE_NAME);
        return client.eval(lua)
                .thenApply(TarantoolResponse::get)
                .thenApply(result -> {
                    if (result != null && !result.isEmpty() && result.get(0) instanceof Number n) {
                        return n.longValue();
                    }
                    return 0L;
                })
                .exceptionally(e -> {
                    log.error("Failed to count records", e);
                    return 0L;
                });
    }

    public CompletableFuture<Object> putAsync(String key, byte[] value) {
        String lua = String.format("return box.space['%s']:replace({...})", SPACE_NAME);
        return client.eval(lua, List.of(key, value))
                .thenApply(r -> null)
                .whenComplete((v, e) -> {
                    if (e == null) {
                        log.debug("Put key: {}", key);
                    } else {
                        log.error("Failed to put key {}", key, e);
                    }
                });
    }

    public CompletableFuture<Optional<?>> getAsync(String key) {
        String lua = String.format(
                "local tuple = box.space['%s']:get({...}) return tuple and tuple[2] or nil",
                SPACE_NAME
        );

        return client.eval(lua, List.of(key))
                .thenApply(TarantoolResponse::get)
                .thenApply(result -> {
                    if (result != null && !result.isEmpty() && result.get(0) != null) {
                        Object val = result.get(0);
                        if (val instanceof byte[] bytes) {
                            return Optional.of(bytes);
                        }
                    }
                    return Optional.empty();
                })
                .exceptionally(e -> {
                    log.error("Failed to get key: {}", key, e);
                    return Optional.empty();
                });
    }

    public CompletableFuture<Boolean> deleteAsync(String key) {
        String lua = "return box.space.KV:delete({...}) ~= nil";

        return client.eval(lua, List.of(key))
                .thenApply(TarantoolResponse::get)
                .thenApply(result -> {
                    boolean deleted = result != null && !result.isEmpty() && result.get(0) != null;
                    log.debug("Delete key={} success={}", key, deleted);
                    return deleted;
                })
                .exceptionally(e -> {
                    log.error("Failed to delete key: {}", key, e);
                    throw new RuntimeException("Delete failed for key: " + key, e);
                });
    }

    public CompletableFuture<List<?>> rangeAsync(String keyStart, String keyEnd, int limit) {
        String lua = """
            local space = box.space['%s']
            local result = {}
            local k_start = unpack({...}) -- получаем keyStart из аргументов
            for _, tuple in space:pairs({k_start}, {iterator = 'GE'}) do
                -- Сравнение строк в Lua требует кавычек, если подставлять текстом.
                -- Но лучше передать и keyEnd как аргумент.
                if tuple[1] > select(2, ...) then break end 
                table.insert(result, {tuple[1], tuple[2]})
                if #result >= select(3, ...) then break end
            end
            return result
            """.formatted(SPACE_NAME);

        return client.eval(lua, List.of(keyStart, keyEnd, limit))
                .thenApply(TarantoolResponse::get)
                .thenApply(rawResult -> {
                    if (rawResult == null) return List.of();

                    return rawResult.stream()
                            .map(item -> {
                                if (!(item instanceof List<?> tuple)) {
                                    return null;
                                }
                                String k = tuple.get(0) != null ? tuple.get(0).toString() : "";
                                byte[] v = null;
                                if (tuple.size() > 1) {
                                    Object valObj = tuple.get(1);
                                    if (valObj instanceof byte[] bytes) {
                                        v = bytes;
                                    } else if (valObj != null) {
                                        v = valObj.toString().getBytes(StandardCharsets.UTF_8);
                                    }
                                }
                                return new KeyValuePair(k, v);
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                })
                .exceptionally(e -> {
                    log.error("Failed to get range from {} to {}", keyStart, keyEnd, e);
                    throw new RuntimeException("Range query failed", e);
                });
    }

    public CompletableFuture<List<?>> rangeAsync(String keyStart, String keyEnd) {
        return rangeAsync(keyStart, keyEnd, 10000);
    }

    public CompletableFuture<Object> truncateAsync() {
        return client.eval("box.space.KV:truncate()")
                .thenApply(r -> null)
                .whenComplete((v, e) -> {
                    if (e == null) {
                        log.info("Space KV truncated");
                    } else {
                        log.error("Failed to truncate space KV", e);
                    }
                });
    }

    public record KeyValuePair(String key, byte[] value) {
    }
}

