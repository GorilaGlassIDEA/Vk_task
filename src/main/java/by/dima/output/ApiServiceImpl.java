package by.dima.output;

import by.dima.grpc.Api;
import by.dima.grpc.ApiServiceGrpc;
import by.dima.input.TarantoolCrudService;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@RequiredArgsConstructor
@GrpcService
@Slf4j
public class ApiServiceImpl extends ApiServiceGrpc.ApiServiceImplBase {

    private final TarantoolCrudService service;

    @Override
    public void count(Api.CountRequest request, StreamObserver<Api.CountResponse> responseObserver) {
        service.callCount().whenComplete((count, throwable) -> {
            if (throwable != null) {
                log.error("Error in count", throwable);
                responseObserver.onError(throwable);
                return;
            }
            Api.CountResponse response = Api.CountResponse.newBuilder()
                    .setCount(count)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("gRPC layer: count returned {}", count);
        });
    }

    @Override
    public void put(Api.PutRequest request, StreamObserver<Api.PutResponse> responseObserver) {
        String key = request.getKey();
        byte[] value = request.getValue().toByteArray();

        service.callPut(key, value).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                log.error("Error in put key={}", key, throwable);
                responseObserver.onError(throwable);
                return;
            }
            Api.PutResponse response = Api.PutResponse.newBuilder().setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("GRPC layer: put key={} success", key);
        });
    }

    @Override
    public void get(Api.GetRequest request, StreamObserver<Api.GetResponse> responseObserver) {
        String key = request.getKey();

        service.callGet(key).whenComplete((value, throwable) -> {
            if (throwable != null) {
                log.error("Error in get key={}", key, throwable);
                responseObserver.onError(throwable);
                return;
            }
            boolean found = value != null && value.length > 0;
            Api.GetResponse response = Api.GetResponse.newBuilder()
                    .setValue(value != null ? ByteString.copyFrom(value) : ByteString.EMPTY)
                    .setFound(found)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("GRPC layer: get key={} found={}", key, found);
        });
    }

    @Override
    public void delete(Api.DeleteRequest request, StreamObserver<Api.DeleteResponse> responseObserver) {
        String key = request.getKey();

        service.callDelete(key).whenComplete((success, throwable) -> {
            if (throwable != null) {
                log.error("Error in delete key={}", key, throwable);
                responseObserver.onError(throwable);
                return;
            }
            Api.DeleteResponse response = Api.DeleteResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("GRPC layer: delete key={} success={}", key, success);
        });
    }

    @Override
    public void range(Api.RangeRequest request, StreamObserver<Api.RangeResponse> responseObserver) {
        String keyStart = request.getKeyStart();
        String keyEnd = request.getKeyEnd();

        service.callRange(keyStart, keyEnd).whenComplete((pairs, throwable) -> {
            if (throwable != null) {
                log.error("Error in range from {} to {}", keyStart, keyEnd, throwable);
                responseObserver.onError(throwable);
                return;
            }
            for (var pair : pairs) {
                Api.RangeResponse response = Api.RangeResponse.newBuilder()
                        .setKey(pair.key())
                        .setValue(pair.value() != null ? ByteString.copyFrom(pair.value()) : ByteString.EMPTY)
                        .build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
            log.info("gRPC layer: range streamed {} items", pairs.size());
        });
    }
}