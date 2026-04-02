package by.dima.input;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TarantoolCrudService {

    private final TarantoolRepository repository;

    public long callCount() {
        long count = repository.count();
        log.info("Tarantool count = {}", count);
        return count;
    }

    public void callPut(String key, byte[] values) {
        repository.put(key, values);
        log.info("Tarantool put with key = {}", key);
    }

    public byte[] callGet(String key) {
        return repository.get(key).orElse(new byte[0]);
    }

    public boolean callDelete(String key) {
        boolean deleted = repository.delete(key);
        log.info("Tarantool delete key={} success={}", key, deleted);
        return deleted;
    }

    public List<TarantoolRepository.KeyValuePair> callRange(String keyStart, String keyEnd) {
        List<TarantoolRepository.KeyValuePair> result = repository.range(keyStart, keyEnd);
        log.info("Tarantool range from '{}' to '{}' returned {} records", keyStart, keyEnd, result.size());
        return result;
    }
}
