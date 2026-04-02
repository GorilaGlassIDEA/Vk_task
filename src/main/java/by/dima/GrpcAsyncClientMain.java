package by.dima;

import by.dima.grpc.Api;
import by.dima.grpc.ApiServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcAsyncClientMain {
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ApiServiceGrpc.ApiServiceStub asyncStub = ApiServiceGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);

        Api.PutRequest request = Api.PutRequest.newBuilder()
                .setKey("async_key")
                .setValue(ByteString.copyFromUtf8("async"))
                .build();

        asyncStub.put(request, new StreamObserver<Api.PutResponse>() {
            @Override
            public void onNext(Api.PutResponse response) {
                // Вызывается, когда сервер прислал ответ
                System.out.println("Ответ получен! Успех: " + response.getSuccess());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Ошибка при выполнении: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Запрос завершен");
                latch.countDown();
            }
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            System.out.println("Таймаут ожидания ответа");
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}