package com.msp.impls;

import com.msp.enums.EActivationPurpose;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.models.ActivationToken;
import com.msp.models.User;
import com.msp.repositories.ActivationTokenRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AccountActivationService;
import com.msp.services.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountActivationServiceImpl implements AccountActivationService {

    private final ActivationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    @Transactional
    public void sendActivation(User user, EActivationPurpose purpose) {
        String token = UUID.randomUUID().toString().replace("-", "");
        ActivationToken row = ActivationToken.builder()
                .token(token)
                .user(user)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepo.save(row);

        String link = frontendUrl.replaceAll("/$", "") + "/activate?token=" + token;
        String roleLabel = switch (purpose) {
            case STORE_OWNER -> "store owner";
            case BRANCH_MANAGER -> "branch manager";
            case CASHIER -> "cashier";
            case STORE_MANAGER -> "store manager";
            default -> "staff";
        };
        mailService.send(
                user.getEmail(),
                "Activate your POSify " + roleLabel + " account",
                """
                <div style="font-family:sans-serif;max-width:560px">
                  <h2>Welcome to POSify</h2>
                  <p>Hi %s, your <b>%s</b> account is ready. Set your password using this link (valid 7 days):</p>
                  <p><a href="%s" style="background:#14B8A6;color:#0F172A;padding:10px 16px;border-radius:8px;text-decoration:none;font-weight:700">Activate account</a></p>
                  <p style="color:#64748b;font-size:12px">If the button does not work, copy: %s</p>
                </div>
                """.formatted(user.getFirstName(), roleLabel, link, link)
        );
    }

    @Override
    @Transactional
    public User activate(String token, String password) {
        ActivationToken row = tokenRepo.findByToken(token)
                .orElseThrow(() -> new UserException("Invalid or expired activation link"));
        if (row.getUsedAt() != null) {
            throw new UserException("This activation link has already been used");
        }
        if (row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserException("This activation link has expired");
        }
        User user = row.getUser();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserStatus(EUserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        row.setUsedAt(LocalDateTime.now());
        tokenRepo.save(row);
        return user;
    }
}
