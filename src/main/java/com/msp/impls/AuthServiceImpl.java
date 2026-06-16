package com.msp.impls;

import com.msp.configs.JwtProvider;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.User;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.response.AuthResponse;
import com.msp.repositories.UserRepository;
import com.msp.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "users")
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final CustomUserImpl customUserImpl;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private JwtProvider provider;

    @Override
    @Caching(
            put = { @CachePut(key = "#result.user.id") },
            evict = {
                    @CacheEvict(value = "users-page", allEntries = true),
                    @CacheEvict(value = "users-by-email", key = "#userDto.email")
            }
    )
    public AuthResponse signup(UserDto userDto) throws UserException {
        User user = userRepo.findByEmail(userDto.getEmail());
        if(user != null) {
            throw new UserException("Email already in use !");
        }
        if(userDto.getRole().equals(EUserRole.ROLE_STORE_ADMIN)) {
            throw new UserException("Role Admin isn't Allowed!");
        }
        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setFirstName(userDto.getFirstName());
        newUser.setLastName(userDto.getLastName());
        newUser.setPhone(userDto.getPhone());
        newUser.setRole(userDto.getRole());
        newUser.setUserStatus(EUserStatus.PENDING);
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepo.save(newUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = provider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Registered Successfully!");
        authResponse.setUser(UserMapper.toDTO(savedUser));
        return authResponse;
    }
    @Override
    @Caching(
            put = { @CachePut(key = "#result.user.id") },
            evict = { @CacheEvict(value = "users-page", allEntries = true) }
    )
    public AuthResponse login(UserDto userDto) {
        log.info("Login attempt for email: {}", userDto.getEmail());

        // Validate input
        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new UserException("Email cannot be empty");
        }
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new UserException("Password cannot be empty");
        }

        String email = userDto.getEmail().trim();
        String password = userDto.getPassword();

        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get and update user
        User user = userRepo.findByEmail(email);
        user.setLastLogin(LocalDateTime.now());
        user = userRepo.save(user);

        // Use the full token (with tenantId + userId) so tenant-scoped users
        // get their tenantId embedded in the JWT from the first login.
        String token = provider.generateToken(authentication, user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Logged In Successfully!");
        authResponse.setUser(UserMapper.toDTO(user));

        log.info("Login successful for: {}", email);
        return authResponse;
    }

    private Authentication authenticate(String email, String password) {
        try {
            UserDetails userDetails = customUserImpl.loadUserByUsername(email);

            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                throw new UserException("Password doesn't match!");
            }

            return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

        } catch (UsernameNotFoundException e) {
            throw new UserException("Email doesn't exist: " + email);
        }
    }
}