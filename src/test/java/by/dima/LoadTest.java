package by.dima;

import by.dima.input.TarantoolCrudService;
import by.dima.input.TarantoolRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = by.dima.Main.class)
@Slf4j
class LoadTest {

    @Autowired
    private TarantoolCrudService service;

    @Autowired
    private TarantoolRepository repository;

    private static final int COUNT = 1000;
    private static final String PREFIX = "test_key_";

    @Test
    void testAllFunctionsWith1000Records() {
        repository.truncate();
        for (int i = 0; i < COUNT; i++) {
            String key = PREFIX + i;
            byte[] value = ("value_" + i).getBytes(StandardCharsets.UTF_8);
            service.callPut(key, value);
        }

        long count = service.callCount();
        assertEquals(COUNT, count, "Count after insert should be " + COUNT);


        byte[] value = service.callGet(PREFIX + "42");
        assertNotNull(value);
        assertEquals("value_42", new String(value, StandardCharsets.UTF_8));


        boolean deleted = service.callDelete(PREFIX + "100");
        assertTrue(deleted);
        assertEquals(0, service.callGet(PREFIX + "100").length);

        List<TarantoolRepository.KeyValuePair> rangeResult = service.callRange(PREFIX + "0", PREFIX + "200");
        assertFalse(rangeResult.isEmpty());
        assertTrue(rangeResult.size() <= 201);
    }
}