-- Seed users. Password for all seeded accounts is "Password123!"
-- No ADMIN is seeded on purpose: the first account to register on a fresh database is promoted to
-- ADMIN by AuthService.register, and a pre-seeded admin would take that bootstrap slot away.
-- BCrypt hash of "Password123!"
INSERT INTO users (id, full_name, email, password_hash, phone_number, role, active, created_at, updated_at)
VALUES (1, 'Asha Sharma', 'asha.customer@vivahspace.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa1Ajxwsyxgxx6NIEmENhCEqiglLZH6q', '9990000001', 'CUSTOMER', true, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password_hash, phone_number, role, active, created_at, updated_at)
VALUES (2, 'Rahul Verma', 'rahul.customer@vivahspace.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa1Ajxwsyxgxx6NIEmENhCEqiglLZH6q', '9990000002', 'CUSTOMER', true, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password_hash, phone_number, role, active, created_at, updated_at)
VALUES (3, 'Priya Nair', 'priya.customer@vivahspace.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa1Ajxwsyxgxx6NIEmENhCEqiglLZH6q', '9990000003', 'CUSTOMER', true, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, password_hash, phone_number, role, active, created_at, updated_at)
VALUES (4, 'Vikram Singh', 'vikram.customer@vivahspace.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa1Ajxwsyxgxx6NIEmENhCEqiglLZH6q', '9990000004', 'CUSTOMER', true, now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
