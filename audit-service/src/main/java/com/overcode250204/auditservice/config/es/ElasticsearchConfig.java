//package com.overcode250204.auditservice.config.es;
//
//import org.apache.hc.core5.http.Header;
//import org.apache.hc.core5.http.HttpHost;
//import org.apache.hc.core5.http.message.BasicHeader;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestClient;
//
//@Configuration
//public class ElasticsearchConfig {
//
//    @Bean
//    public RestClient restClient() {
//        return RestClient.builder(new HttpHost("localhost", 9200))
//                .setDefaultHeaders(new Header[]{
//                        new BasicHeader("Accept", "application/vnd.elasticsearch+json;compatible-with=8"),
//                        new BasicHeader("Content-Type", "application/vnd.elasticsearch+json;compatible-with=8")
//                })
//                .build();
//    }
//}
