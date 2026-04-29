package com.sep490.ecoverse_be.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình kết nối đến Qdrant vector database qua gRPC.
 */
@Configuration
@Slf4j
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.grpc-port}")
    private int grpcPort;

    @Bean
    public QdrantClient qdrantClient() {
        log.info("Khởi tạo Qdrant client → {}:{}", host, grpcPort);
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(host, grpcPort, false).build();
        return new QdrantClient(grpcClient);
    }
}
