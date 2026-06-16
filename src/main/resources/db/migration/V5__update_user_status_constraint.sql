ALTER TABLE users
DROP CONSTRAINT users_user_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_user_status_check
        CHECK (
            user_status IN (
                            'ACTIVE',
                            'SUSPENDED',
                            'DISCHARGED',
                            'PENDING'
                )
            );