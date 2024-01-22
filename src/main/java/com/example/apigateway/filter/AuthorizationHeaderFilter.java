package com.example.apigateway.filter;

import com.netflix.discovery.converters.Auto;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final Environment env;

    @Autowired
    public AuthorizationHeaderFilter(Environment env) {
        super(Config.class);
        this.env = env;
    }

    //login -> token반환 -> token을 헤더에 포함해 요청 -> 서버에서는 token 유효성 확인
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);

            List<String> headers = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
            String authorizationHeader = headers.get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

//            List<String> userIdHeaders = request.getHeaders().getOrEmpty("userId");
//            String userIdHeader = userIdHeaders.get(0);

            if (!isJwtValid(jwt)) {
                return onError(exchange, "jwt token is not valid", HttpStatus.UNAUTHORIZED);
            }

            //custom post filter. suppose we can call error response handler based on error code.
            return chain.filter(exchange);
        };
    }

    //Mono, Flux == Spring WebFlux
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        log.error(err);
        return response.setComplete();
    }

    private boolean isJwtValid(String jwt) {
        boolean returnValue = true;

        String subject = null;
        log.debug("env.getProperty(token.secret)={}",env.getProperty("token.secret"));
        try {
            subject = Jwts.parser() //JwtParser 인스턴스 생성
                    //JWT의 서명을 검증하는 데 사용되는 키를 설정
                    .setSigningKey(env.getProperty("token.secret"))
                    //전달된 문자열 형태의 JWT를 파싱하고 검증
                    //이 과정에서 JWT의 형식, 서명의 유효성, 만료 시간 등이 검증됨
                    //전달받은 jwt에서 header와 payload를 token.secret으로 전자서명한 결과가
                    //전달받은 jwt의 페이로드와 같은지를 보는듯?
                    //성공적으로 검증되면 Jws<Claims> 객체를 반환
                    .parseClaimsJws(jwt)
                    //Jws<Claims> 객체에서 getBody 메서드를 호출하여 JWT의 payload 부분을 나타내는 Claims 객체를 얻음
                    //Claims 객체는 JWT에 포함된 다양한 클레임(예: 사용자 ID, 역할, 만료 시간 등)에 접근할 수 있게 해줌
                    .getBody()
                    //getSubject 메서드를 호출하여 JWT의 sub 클레임을 얻음
                    //sub 클레임은 일반적으로 사용자를 식별하는 데 사용되는 고유 식별자임
                    .getSubject();
        } catch (Exception ex) {
            returnValue = false;
        }

        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }

    public static class Config {

    }
}
