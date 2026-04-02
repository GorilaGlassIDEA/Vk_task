package by.dima.input;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarantoolCrudService {

    private final AsyncTarantoolRepository repository;

    public CompletableFuture<Long> callCount() {
        return repository.count()
                .thenApply(count -> {
                    log.info("Tarantool count = {}", count);
                    return count;
                });
    }

    public CompletableFuture<Void> callPut(String key, byte[] values) {
        return repository.put(key, values)
                .thenAccept(unused -> log.info("Tarantool put with key = {}", key));
    }

    public CompletableFuture<byte[]> callGet(String key) {
        return repository.get(key)
                .thenApply(opt -> {
                    byte[] result = (byte[]) opt.orElse(null);
                    log.debug("Get key {}: found={} bytes", key, result.length);
                    return result;
                });
    }

    public CompletableFuture<Boolean> callDelete(String key) {
        return repository.delete(key)
                .thenApply(deleted -> {
                    log.info("Tarantool delete key={} success={}", key, deleted);
                    return deleted;
                });
    }


    public CompletableFuture<List<AsyncTarantoolRepository.KeyValuePair>> callRange(String keyStart, String keyEnd) {
        return repository.range(keyStart, keyEnd)
                .thenApply(result -> {
                    log.info("Tarantool range from '{}' to '{}' returned {} records", keyStart, keyEnd, result.size());
                    return (List<AsyncTarantoolRepository.KeyValuePair>) (List<?>) result;
                });
    }
}