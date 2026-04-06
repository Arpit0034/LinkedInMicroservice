package com.linkedInProject.postsService.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper() ;
    }
    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder(new SpringEncoder(HttpMessageConverters::new));
    }
}
