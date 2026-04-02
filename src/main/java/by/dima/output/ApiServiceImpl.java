package by.dima.output;

import by.dima.grpc.Api;
import by.dima.grpc.ApiServiceGrpc;
import by.dima.input.TarantoolCrudService;
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
            byte[] values = request.getValue().toByteArray();
        } catch (Exception e) {

        }
    }
}
