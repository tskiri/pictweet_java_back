package in.tech_camp.pictweet;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import in.tech_camp.pictweet.custom_user.CustomUserDetail;
import jakarta.servlet.http.HttpServletResponse;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors
                    .configurationSource(request -> {
                        var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                        corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
                        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                        corsConfiguration.setAllowCredentials(true);
                        corsConfiguration.setAllowedHeaders(List.of("*"));
                        return corsConfiguration;
                    })
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        //ログイン不要なHTTP GETリクエスト
                        .requestMatchers(HttpMethod.GET, "/css/**", "/images/**", "/tweets/{id:[0-9]+}", "/users/{id:[0-9]+}", "/tweets/search", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tweets/").permitAll()
                        //ログイン不要なHTTP POSTリクエスト
                        .requestMatchers(HttpMethod.POST, "/api/users/", "/api/login").permitAll()
                        .anyRequest().authenticated())
                        //上記以外のリクエストは認証されたユーザーのみ許可されます(要ログイン)

                // ログイン時の処理
                .formLogin(login -> login
                    // ログインのAPIパスを定義
                    .loginProcessingUrl("/api/login")
                    .usernameParameter("email")
                    // ログイン成功時の処理（記述が長くなるので、authenticationSuccessHandler()として外側で定義している）
                    .successHandler(authenticationSuccessHandler())
                    // ログイン失敗時の処理
                    .failureHandler((request, response, exception) -> {
                        // レスポンスにSC_UNAUTHORIZEDというステータスを設定
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        // JSON形式のレスポンスを設定
                        response.setContentType("application/json");
                        // 文字コードを設定
                        response.setCharacterEncoding("UTF-8");
                        // レスポンスに含めるエラーメッセージを設定
                        response.getWriter().write(
                            "{\"error\":\"Invalid credentials\"}"
                        );
                    })
                )

                // ログアウト時の処理
                .logout(logout -> logout
                    .logoutUrl("/api/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        // レスポンスにSC_OKというステータスを設定
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"success\":true}");
                    })
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            // authentication.getPrincipal()でログイン中のユーザー情報を取得し、レスポンスにその情報を含めるようにしている
            CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format(
                "{\"id\":%d,\"nickname\":\"%s\",\"email\":\"%s\"}",
                userDetails.getId(),
                userDetails.getNickname(),
                userDetails.getUsername()
            ));
        };
    }
}
