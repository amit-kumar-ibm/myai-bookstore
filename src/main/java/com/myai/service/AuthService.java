package com.myai.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import com.myai.config.JwtProperties;
import com.myai.dto.ApiDtos.AuthResponse;
import com.myai.dto.ApiDtos.LoginRequest;
import com.myai.dto.ApiDtos.RegisterRequest;
import com.myai.dto.DtoMapper;
import com.myai.exception.ApiException;
import com.myai.model.User;
import com.myai.repository.UserRepository;

@Service
public class AuthService {

    /** Same message for unknown user and bad password, to avoid account enumeration. */
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtEncoder jwtEncoder,
            JwtProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("Email is already registered");
        }
        User user = users.save(new User(req.email().toLowerCase(), encoder.encode(req.password()), req.name()));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private AuthResponse issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("myai-bookstore")
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.ttlSeconds()))
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AuthResponse(token, "Bearer", props.ttlSeconds(), DtoMapper.toDto(user));
    }
}
