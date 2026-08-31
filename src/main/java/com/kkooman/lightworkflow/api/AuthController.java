package com.kkooman.lightworkflow.api;

import com.kkooman.lightworkflow.security.JwtTokenProvider;
import com.kkooman.lightworkflow.service.TokenStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;

    public AuthController(AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            TokenStore tokenStore) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            String token = tokenProvider.createToken(authentication);
            tokenStore.store(token);
            return ResponseEntity.ok(ApiResponse.success(new AuthResponse(token), "로그인 성공"));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.fail("AUTH_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }
}
