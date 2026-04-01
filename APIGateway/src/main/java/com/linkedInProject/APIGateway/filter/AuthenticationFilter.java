package com.linkedInProject.APIGateway.filter;

import com.linkedInProject.APIGateway.service.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtService jwtService ;
    public AuthenticationFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService ;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            log.info("Auth request: {}",exchange.getRequest().getURI());
            final String tokenHeader = exchange.getRequest().getHeaders().getFirst("Authorization") ;
            if(tokenHeader == null || !tokenHeader.startsWith("Bearer")){
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            final String token = tokenHeader.split("Bearer ")[1] ;
            try{
                String userId = jwtService.getUserIdFromToken(token);
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id",userId))
                        .build();
                return chain.filter(mutatedExchange) ;
            }catch (JwtException e){
                log.info("JWT Exception {}",e.getLocalizedMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        });
    }

    public static class Config{}

}
// ServerWebExchange contains the current HTTP request and response details.
// It allows us to read, modify, and pass request data in a reactive way.
// ye incoming request ko hold karta hai, response ko bhi hold karta hai,aur extra data bhi store kar sakta hai.
// ServerWebExchange us request ka poora packet hai: request data, response data,headers,path,query params,attributes.

// GatewayFilterChain represents the next filters in the gateway pipeline.
// Calling chain.filter(exchange) passes the request to the next filter or route.
// Jab request gateway me aati hai, to ek filter usko process karta hai, phir next filter ko pass karta hai. chain.filter(exchange) ka matlab hota hai: “ab next filter ko de do”.

// Config is a placeholder for custom filter settings.
// It can be expanded later to accept route-specific values from configuration.
// Config ek empty place holder class hai jisme future me filter ke settings ya route-specific values add kar sakte hain.
// example user route ke liye header ka naam X-User-Id ho ,admin route ke liye header ka naam X-Admin-Id ho