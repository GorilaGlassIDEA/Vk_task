package by.dima;

import by.dima.grpc.Api;
import by.dima.grpc.ApiServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.nio.charset.StandardCharsets;

public class TestFullGrpc {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ApiServiceGrpc.ApiServiceBlockingStub stub = ApiServiceGrpc.newBlockingStub(channel);

        System.out.println("=== Тестирование gRPC API ===\n");

        // 1. Проверяем начальный count
        long initialCount = stub.count(Api.CountRequest.newBuilder().build()).getCount();
        System.out.println("1. Начальный count: " + initialCount);

        // 2. Добавляем несколько записей через put (отправляем как байты)
        System.out.println("\n2. Добавляем записи:");

        stub.put(Api.PutRequest.newBuilder()
                .setKey("user1")
                .setValue(com.google.protobuf.ByteString.copyFrom("Alice".getBytes(StandardCharsets.UTF_8)))
                .build());
        System.out.println("   - Добавлен user1 = Alice (байты)");

        stub.put(Api.PutRequest.newBuilder()
                .setKey("user2")
                .setValue(com.google.protobuf.ByteString.copyFrom("Bob".getBytes(StandardCharsets.UTF_8)))
                .build());
        System.out.println("   - Добавлен user2 = Bob (байты)");

        stub.put(Api.PutRequest.newBuilder()
                .setKey("user3")
                .setValue(com.google.protobuf.ByteString.copyFrom("Charlie".getBytes(StandardCharsets.UTF_8)))
                .build());
        System.out.println("   - Добавлен user3 = Charlie (байты)");

        // 3. Проверяем count после добавления
        long afterPutCount = stub.count(Api.CountRequest.newBuilder().build()).getCount();
        System.out.println("\n3. Count после добавления: " + afterPutCount);

        // 4. Получаем значения через get (получаем байты и конвертируем в строку для вывода)
        System.out.println("\n4. Получение значений:");

        Api.GetResponse get1 = stub.get(Api.GetRequest.newBuilder().setKey("user1").build());
        String value1 = get1.getFound() ? get1.getValue().toStringUtf8() : "не найден";
        System.out.println("   - user1: " + value1);

        Api.GetResponse get2 = stub.get(Api.GetRequest.newBuilder().setKey("user2").build());
        String value2 = get2.getFound() ? get2.getValue().toStringUtf8() : "не найден";
        System.out.println("   - user2: " + value2);

        Api.GetResponse get3 = stub.get(Api.GetRequest.newBuilder().setKey("user3").build());
        String value3 = get3.getFound() ? get3.getValue().toStringUtf8() : "не найден";
        System.out.println("   - user3: " + value3);

        // 5. Обновляем значение
        System.out.println("\n5. Обновление значения:");
        stub.put(Api.PutRequest.newBuilder()
                .setKey("user2")
                .setValue(com.google.protobuf.ByteString.copyFrom("Robert".getBytes(StandardCharsets.UTF_8)))
                .build());
        System.out.println("   - user2 обновлен на Robert (байты)");

        Api.GetResponse updatedGet = stub.get(Api.GetRequest.newBuilder().setKey("user2").build());
        System.out.println("   - Проверка: user2 = " + updatedGet.getValue().toStringUtf8());

        // 6. Добавляем запись с null значением
        System.out.println("\n6. Добавление null значения:");
        stub.put(Api.PutRequest.newBuilder()
                .setKey("null_key")
                .setValue(com.google.protobuf.ByteString.EMPTY)
                .build());
        System.out.println("   - Добавлен null_key с null значением");

        // 7. Удаляем запись
        System.out.println("\n7. Удаление записи:");
        stub.delete(Api.DeleteRequest.newBuilder().setKey("user1").build());
        System.out.println("   - user1 удален");

        // 8. Проверяем финальный count
        long finalCount = stub.count(Api.CountRequest.newBuilder().build()).getCount();
        System.out.println("\n8. Финальный count: " + finalCount);

        // 9. Проверяем range запрос
        System.out.println("\n9. Range запрос (от user1 до user3):");
        java.util.Iterator<Api.RangeResponse> rangeResp = stub.range(Api.RangeRequest.newBuilder()
                .setKeyStart("user1")
                .setKeyEnd("user3")
                .build());

        while (rangeResp.hasNext()) {
            Api.RangeResponse resp = rangeResp.next();
            String value = resp.getValue().isEmpty() ? "null" : resp.getValue().toStringUtf8();
            System.out.println("   - " + resp.getKey() + " = " + value);
        }

        channel.shutdown();
        System.out.println("\n=== Тестирование завершено ===");
    }
}