package com.msp.repositories;

import com.msp.enums.EUserStatus;
import com.msp.models.Store;
import com.msp.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByEmail(String email);

    /**
     * Email is not unique in the schema. Prefer ACTIVE super-admin, then any ACTIVE,
     * then the first match — so duplicate / PENDING rows do not block login.
     */
    default User findByEmail(String email) {
        List<User> matches = findAllByEmail(email);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.stream()
                .filter(u -> u.getRole() == com.msp.enums.EUserRole.ROLE_SUPER_ADMIN
                        && u.getUserStatus() == EUserStatus.ACTIVE)
                .findFirst()
                .or(() -> matches.stream()
                        .filter(u -> u.getUserStatus() == EUserStatus.ACTIVE)
                        .findFirst())
                .orElse(matches.get(0));
    }
    Page<User> findByStore(Store store, Pageable pageable);
    Page<User> findByBranchId(UUID branchId,Pageable pageable);
    Page<User> findByUserStatus(EUserStatus status, Pageable pageable);
}
