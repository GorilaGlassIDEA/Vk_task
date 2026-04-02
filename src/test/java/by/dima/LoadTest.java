package by.dima;

import by.dima.input.AsyncTarantoolRepository;
import by.dima.input.TarantoolCrudService;
import by.dima.input.TarantoolRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = by.dima.Main.class)
@Slf4j
class LoadTest {

    @Autowired
    private TarantoolCrudService service;

    @Autowired
    private TarantoolRepository repository;

    @Autowired
    private AsyncTarantoolRepository asyncRepo;

    private static final int COUNT = 1000;
    private static final String PREFIX = "test_key_";


    @Test
    void testAllFunctionsWith1000Records() throws Exception {
        asyncRepo.truncateAsync().get(10, TimeUnit.SECONDS);

        CompletableFuture[] putFutures = new CompletableFuture[COUNT];

        for (int i = 0; i < COUNT; i++) {
            String key = PREFIX + i;
            byte[] value = ("value_" + i).getBytes(StandardCharsets.UTF_8);
            putFutures[i] = asyncRepo.putAsync(key, value);
        }

        CompletableFuture.allOf(putFutures).get(30, TimeUnit.SECONDS);

        long count = asyncRepo.countAsync().get(5, TimeUnit.SECONDS);
        assertEquals(COUNT, count, "Count after insert should be " + COUNT);

        Optional<byte[]> valueOpt = (Optional<byte[]>) asyncRepo.getAsync(PREFIX + "42").get(5, TimeUnit.SECONDS);
        assertTrue(valueOpt.isPresent());
        assertEquals("value_42", new String(valueOpt.get(), StandardCharsets.UTF_8));

        boolean deleted = asyncRepo.deleteAsync(PREFIX + "100").get(5, TimeUnit.SECONDS);
        assertTrue(deleted);

        Optional<byte[]> afterDelete = (Optional<byte[]>) asyncRepo.getAsync(PREFIX + "100").get(5, TimeUnit.SECONDS);
        assertTrue(afterDelete.isEmpty() || afterDelete.get().length == 0);
        List<AsyncTarantoolRepository.KeyValuePair> rangeResult =
                (List<AsyncTarantoolRepository.KeyValuePair>) asyncRepo.rangeAsync(PREFIX + "0", PREFIX + "200").get(10, TimeUnit.SECONDS);

        assertFalse(rangeResult.isEmpty());
        assertTrue(rangeResult.size() <= 201);
    }
}