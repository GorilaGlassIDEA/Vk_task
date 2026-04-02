package by.dima.output;

import by.dima.grpc.Api;
import by.dima.grpc.ApiServiceGrpc;
import by.dima.input.TarantoolCrudService;
import by.dima.input.TarantoolRepository;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.info.BuildProperties;

import java.util.List;

@RequiredArgsConstructor
@GrpcService
@Slf4j
public class ApiServiceImpl extends ApiServiceGrpc.ApiServiceImplBase {

    private final TarantoolCrudService service;

    @Override
    public void count(Api.CountRequest request, StreamObserver<Api.CountResponse> responseObserver) {
        try {
            long count = service.callCount();
            Api.CountResponse response = Api.CountResponse.newBuilder()
                    .setCount(count)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("gRPC layer: count returned {}", count);
        } catch (Exception e) {
            log.error("Error in count", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void put(Api.PutRequest request, StreamObserver<Api.PutResponse> responseObserver) {
        try {
            String key = request.getKey();
            byte[] value = request.getValue().toByteArray();

            byte[] cleanValue = value.length == 0 ? null : value;
            service.callPut(key, value);

            Api.PutResponse response = Api.PutResponse.newBuilder().setSuccess(true).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("GRPC layer: put key={} length_value={}", key, cleanValue != null ? cleanValue.length : 0);

        } catch (Exception e) {
            log.error("Error in put", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void get(Api.GetRequest request, StreamObserver<Api.GetResponse> responseObserver) {
        String key = request.getKey();

        byte[] value = service.callGet(key);
        Api.GetResponse.Builder responseBuilder = Api.GetResponse.newBuilder();

        if (value != null) {
            responseBuilder.setValue(ByteString.copyFrom(value)).setFound(true);
        } else {
            responseBuilder.setValue(ByteString.EMPTY).setFound(false);
        }
        Api.GetResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("GRPC layer: get key={} length_value={}", key, response.getValue().size());

    }

    @Override
    public void delete(Api.DeleteRequest request, StreamObserver<Api.DeleteResponse> responseObserver) {
        try {

            String key = request.getKey();
            boolean success = service.callDelete(key);

            Api.DeleteResponse response = Api.DeleteResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("GRPC layer: delete key={} success={}", key, success);

        } catch (Exception e) {
            log.error("Error in delete key={}", request.getKey(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void range(Api.RangeRequest request, StreamObserver<Api.RangeResponse> responseObserver) {
        String keyStart = request.getKeyStart();
        String keyEnd = request.getKeyEnd();

        try {

            List<TarantoolRepository.KeyValuePair> pairs = service.callRange(keyStart, keyEnd);

            for (TarantoolRepository.KeyValuePair pair : pairs) {
                Api.RangeResponse response = Api.RangeResponse.newBuilder().setKey(pair.key()).setValue(
                        pair.value() != null ? ByteString.copyFrom(pair.value()) : ByteString.EMPTY).build();
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
            log.info("gRPC layer: range from '{}' to '{}' streamed {} items", keyStart, keyEnd, pairs.size());
        } catch (Exception e) {
            log.error("Error in range from {} to {}", keyStart, keyEnd, e);
            responseObserver.onError(e);
        }
    }
}
