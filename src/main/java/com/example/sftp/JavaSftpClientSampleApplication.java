package com.example.sftp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaSftpClientSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSftpClientSampleApplication.class, args);
    }
}
