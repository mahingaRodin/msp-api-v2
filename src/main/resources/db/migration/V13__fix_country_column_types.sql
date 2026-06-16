-- V13__fix_country_column_types.sql
-- Hibernate maps @Column(length = 2) to VARCHAR(2), not PostgreSQL CHAR(2).

ALTER TABLE businesses
    ALTER COLUMN country TYPE VARCHAR(2) USING country::varchar;

ALTER TABLE tenant_registrations
    ALTER COLUMN country TYPE VARCHAR(2) USING country::varchar;
