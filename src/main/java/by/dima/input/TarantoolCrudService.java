package by.dima.input;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        return repository.get(key).orElse(null);
    }


}
