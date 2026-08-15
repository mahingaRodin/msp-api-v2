package com.msp.impls;

import com.msp.configs.JwtProvider;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.Customer;
import com.msp.models.User;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.response.AuthResponse;
import com.msp.repositories.CustomerRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AccountActivationService;
import com.msp.services.AuthService;
import com.msp.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final CustomUserImpl customUserImpl;
    private final UserRepository userRepo;
    private final JwtProvider provider;
    private final MailService mailService;
    private final AccountActivationService activationService;
    private final CustomerRepository customerRepo;

    @Override
    @Transactional
    public AuthResponse signup(UserDto userDto) throws UserException {
        if (userDto.getEmail() == null || userDto.getPassword() == null) {
            throw new UserException("Email and password are required");
        }
        User existing = userRepo.findByEmail(userDto.getEmail());
        if (existing != null) {
            throw new UserException("Email already in use !");
        }
        if (customerRepo.existsByEmail(userDto.getEmail())) {
            throw new UserException("Email already in use !");
        }

        User newUser = new User();
        newUser.setEmail(userDto.getEmail().trim());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setFirstName(userDto.getFirstName());
        newUser.setLastName(userDto.getLastName());
        newUser.setPhone(userDto.getPhone());
        newUser.setRole(EUserRole.ROLE_CUSTOMER);
        newUser.setUserStatus(EUserStatus.PENDING);
        newUser.setEmailVerified(false);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepo.save(newUser);

        Customer customer = Customer.builder()
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(EUserRole.ROLE_CUSTOMER)
                .password(savedUser.getPassword())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        customerRepo.save(customer);

        issueOtp(savedUser);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(null);
        authResponse.setMessage("We sent a 6-digit code to " + savedUser.getEmail() + ". Verify to finish signup.");
        authResponse.setUser(UserMapper.toDTO(savedUser));
        return authResponse;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(String email, String otp) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserException("No account found for that email");
        }
        if (user.isEmailVerified() && user.getUserStatus() == EUserStatus.ACTIVE) {
            return issueJwt(user, "Already verified");
        }
        if (user.getOtpHash() == null || user.getOtpExpiresAt() == null
                || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserException("OTP expired. Request a new code.");
        }
        if (!passwordEncoder.matches(otp, user.getOtpHash())) {
            throw new UserException("Invalid verification code");
        }
        user.setEmailVerified(true);
        user.setUserStatus(EUserStatus.ACTIVE);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        user.setLastLogin(LocalDateTime.now());
        user = userRepo.save(user);
        return issueJwt(user, "Email verified. Welcome to POSify.");
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserException("No account found for that email");
        }
        if (user.isEmailVerified()) {
            throw new UserException("This account is already verified");
        }
        issueOtp(user);
    }

    @Override
    @Transactional
    public AuthResponse activateAccount(String token, String password) {
        if (password == null || password.length() < 6) {
            throw new UserException("Password must be at least 6 characters");
        }
        User user = activationService.activate(token, password);
        return issueJwt(user, "Account activated. You can now use POSify.");
    }

    @Override
    @Transactional
    public AuthResponse login(UserDto userDto) {
        log.info("Login attempt for email: {}", userDto.getEmail());

        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new UserException("Email cannot be empty");
        }
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new UserException("Password cannot be empty");
        }

        String email = userDto.getEmail().trim();
        String password = userDto.getPassword();

        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserException("Email doesn't exist: " + email);
        }
        if (!user.isEmailVerified() && user.getRole() == EUserRole.ROLE_CUSTOMER) {
            throw new UserException("Verify your email first. Check your inbox for the OTP.");
        }
        if (user.getUserStatus() == EUserStatus.PENDING) {
            throw new UserException("Activate your account using the link we emailed you.");
        }
        if (user.getUserStatus() != EUserStatus.ACTIVE) {
            throw new UserException("Account is " + user.getUserStatus().toString().toLowerCase());
        }

        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user.setLastLogin(LocalDateTime.now());
        user = userRepo.save(user);
        return issueJwt(user, "Logged In Successfully!");
    }

    private void issueOtp(User user) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        user.setOtpHash(passwordEncoder.encode(otp));
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepo.save(user);
        mailService.send(
                user.getEmail(),
                "Your POSify verification code",
                """
                <div style="font-family:sans-serif;max-width:560px">
                  <h2>Verify your POSify account</h2>
                  <p>Hi %s, use this code within 10 minutes:</p>
                  <p style="font-size:28px;letter-spacing:8px;font-weight:700">%s</p>
                </div>
                """.formatted(user.getFirstName(), otp)
        );
    }

    private AuthResponse issueJwt(User user, String message) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                java.util.List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = provider.generateToken(authentication, user);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage(message);
        authResponse.setUser(UserMapper.toDTO(user));
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
