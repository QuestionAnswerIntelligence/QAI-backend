package com.power.likelion.common.config;


import com.power.likelion.filter.JwtAuthenticationFilter;
import com.power.likelion.utils.jwts.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ID, Password 문자열을 Base64로 인코딩하여 전달하는 구조
                .httpBasic().disable()
                // 쿠키 기반이 아닌 JWT 기반이므로 사용 X
                .csrf().disable()
                //cors 설정 --> 나중에 오류로 고생할 수있음
                .cors().configurationSource(corsConfigurationSource())
                .and()
                // Spring Security 세션 정책 : 세션을 생성 및 사용하지 않음
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 조건별로 요청 허용/제한 설정
                .authorizeRequests()

                //API 명세서 관련된 모든 요소들도 모두 승인
                .antMatchers("/", "/swagger-ui/**", "/v3/**","/swagger-ui.html").permitAll()
                // 회원가입과 로그인은 모두 승인
                .antMatchers("/login", "/sign-up","/questions/**").permitAll()   // permitAll()을 하게되면 JWT 필터를 거치지 않고 간다.
                .antMatchers("/questions/create").hasRole("USER")
                // /admin으로 시작하는 요청은 ADMIN 권한이 있는 유저에게만 허용
                .antMatchers("/admin/**").hasRole("ADMIN")
                // /user 로 시작하는 요청은 USER 권한이 있는 유저에게만 허용
                .antMatchers("/user/**").hasRole("USER")

                .anyRequest().authenticated() // 위에서 설정한 API를 제외하고는 모두 JWT 필터를 거친다는 소리 TODO 나중에 바꿔야 함

                .and()
                // JWT 인증 필터 적용
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                // 에러 핸들링
                .exceptionHandling()
                .accessDeniedHandler(new AccessDeniedHandler() { // 권한이 없는경우 처리 즉 인증은 됬지만 role의 권한이 없는 것
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        // 권한 문제가 발생했을 때 이 부분을 호출한다.
                        response.setStatus(403);
                        response.setCharacterEncoding("utf-8");
                        response.setContentType("text/html; charset=UTF-8");
                        response.getWriter().write("권한이 없는 사용자입니다.");
                    }
                })
                .authenticationEntryPoint(new AuthenticationEntryPoint() { // 인증이 안된 사용자를 예외처리 하는 것
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        // 인증문제가 발생했을 때 이 부분을 호출한다.
                        response.setStatus(401);
                        response.setCharacterEncoding("utf-8");
                        response.setContentType("text/html; charset=UTF-8");
                        response.getWriter().write("인증되지 않은 사용자입니다.");
                    }
                });

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        // 모든 도메인으로부터의 요청을 허용함
        configuration.setAllowedOrigins(Arrays.asList("*")); // 모든 도메인 허용

        // 모든 헤더를 허용
        configuration.setAllowedMethods(Arrays.asList("*")); // 모든 HTTP 메서드 허용

        // 모든 종류의 HTTP 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));

        //요청에 사용자 인증 정보를 포함할 수 있음 예를 들어 jwt나 세션기반 인증
        //configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 구성 적용
        return source;
    }
}


// CORS 설정



// TODO 나중에 해야 할것 : CORS 공부 심화적으로 하기, Spring Security 다시한번 제대로 공부하기



