INSERT INTO coupons (id, code, discount_percent, active, expiry_date)
VALUES (1, 'WELCOME10', 10.00, true, CURRENT_DATE + 365)
ON CONFLICT (id) DO NOTHING;

INSERT INTO coupons (id, code, discount_percent, active, expiry_date)
VALUES (2, 'FESTIVE20', 20.00, true, CURRENT_DATE + 90)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('coupons', 'id'), (SELECT MAX(id) FROM coupons));

INSERT INTO seasonal_pricing_rules (id, name, start_date, end_date, multiplier)
VALUES (1, 'Wedding Season Peak', CURRENT_DATE, CURRENT_DATE + 180, 1.15)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('seasonal_pricing_rules', 'id'), (SELECT MAX(id) FROM seasonal_pricing_rules));

INSERT INTO proposals (id, user_id, hall_id, hall_base_price, status, estimated_total, coupon_code, created_at, updated_at)
VALUES (1, 2, 1, 250000.00, 'DRAFT', 250000.00, NULL, now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('proposals', 'id'), (SELECT MAX(id) FROM proposals));
