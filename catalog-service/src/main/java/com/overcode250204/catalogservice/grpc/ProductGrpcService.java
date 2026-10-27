// package com.overcode250204.catalogservice.grpc;

// import com.overcode250204.catalogservice.repository.ProductRepository;
// import com.overcode250204.common.grpc.ProductGrpcServiceGrpc;
// import com.overcode250204.common.grpc.ProductRequest;
// import com.overcode250204.common.grpc.ProductResponse;
// import io.grpc.Status;
// import io.grpc.stub.StreamObserver;
// import net.devh.boot.grpc.server.service.GrpcService;

// import java.util.UUID;

// @GrpcService
// public class ProductGrpcService extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {
//     private final ProductRepository productRepository;

//     public ProductGrpcService(ProductRepository productRepository) {
//         this.productRepository = productRepository;
//     }

//     @Override
//     public void getProductInfo(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
//         final UUID tenantId;
//         final UUID productId;
//         try {
//             tenantId = UUID.fromString(request.getTenantId());
//             productId = UUID.fromString(request.getProductId());
//         } catch (IllegalArgumentException exception) {
//             responseObserver.onError(Status.INVALID_ARGUMENT
//                     .withDescription("tenantId and productId must be UUIDs")
//                     .withCause(exception)
//                     .asRuntimeException());
//             return;
//         }

//         ProductResponse response = productRepository.findByIdAndTenantId(productId, tenantId)
//                 .map(product -> ProductResponse.newBuilder()
//                         .setActive(product.isActive())
//                         .setPrice(product.getPrice().doubleValue())
//                         .setSku(product.getSku())
//                         .setName(product.getName())
//                         .build())
//                 .orElseGet(() -> ProductResponse.newBuilder().setActive(false).build());

//         responseObserver.onNext(response);
//         responseObserver.onCompleted();
//     }
// }
