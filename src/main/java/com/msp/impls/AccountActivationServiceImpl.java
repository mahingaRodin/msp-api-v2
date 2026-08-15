package com.msp.impls;

import com.msp.enums.EActivationPurpose;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mail.EmailLayout;
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

    @Value("${app.frontend-url:https://multi-tenant-pos-fe-v2.vercel.app}")
    private String frontendUrl;

    @Override
    @Transactional
    public void sendActivation(User user, EActivationPurpose purpose) {
        String token = UUID.randomUUID().toString().replace("-", "");
        ActivationToken row = ActivationToken.builder()
                .token(token)
                .user(user)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusHours(24))
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
                EmailLayout.wrap(
                        "Welcome to POSify",
                        "<p>Hi %s, your <b>%s</b> account is ready. Set your password using this link (valid 24 hours):</p>%s%s"
                                .formatted(
                                        user.getFirstName(),
                                        roleLabel,
                                        EmailLayout.button(link, "Activate account"),
                                        EmailLayout.muted("If the button does not work, copy: " + link)
                                )
                )
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
