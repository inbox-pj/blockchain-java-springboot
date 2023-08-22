package com.clover.blockchain;

import com.clover.blockchain.block.Block;
import com.clover.blockchain.transaction.TXInput;
import com.clover.blockchain.transaction.TXOutput;
import com.clover.blockchain.transaction.Transaction;
import com.esotericsoftware.kryo.kryo5.Kryo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.HashMap;

@SpringBootApplication
@EnableSwagger2
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Kryo kryo() {
        Kryo kryo = new Kryo();
        kryo.register(HashMap.class);
        kryo.register(Block.class);
        kryo.register(byte[].class);
        kryo.register(Transaction.class);
        kryo.register(Transaction[].class);
        kryo.register(TXInput[].class);
        kryo.register(TXInput.class);
        kryo.register(TXOutput[].class);
        kryo.register(TXOutput.class);
        return kryo;
    }

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.clover.blockchain"))
                .paths(PathSelectors.any()).build()
                .apiInfo(new ApiInfoBuilder().version("1.0").title("Blockchain Service API").description("Blockchain Service API v1.0").build());
    }

}
