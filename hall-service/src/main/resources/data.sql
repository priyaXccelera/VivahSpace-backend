INSERT INTO halls (id, owner_name, name, location, description, capacity, base_price, verification_status, created_at, updated_at)
VALUES (1, 'Meera Events', 'Grand Regency Hall', 'Mumbai', 'A luxurious hall for grand weddings', 500, 250000.00, 'VERIFIED', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO halls (id, owner_name, name, location, description, capacity, base_price, verification_status, created_at, updated_at)
VALUES (2, 'Sunrise Banquets', 'Sunrise Garden Venue', 'Pune', 'Open-air garden venue', 300, 150000.00, 'PENDING', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO halls (id, owner_name, name, location, description, capacity, base_price, verification_status, created_at, updated_at)
VALUES (3, 'Royal Palace Events', 'Royal Palace Hall', 'Delhi', 'Palace-themed banquet hall', 800, 400000.00, 'VERIFIED', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO halls (id, owner_name, name, location, description, capacity, base_price, verification_status, created_at, updated_at)
VALUES (4, 'City Center Events', 'City Center Hall', 'Bengaluru', 'Modern hall in the city center', 200, 100000.00, 'UNVERIFIED', now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('halls', 'id'), (SELECT MAX(id) FROM halls));

INSERT INTO hall_availability_slots (id, hall_id, slot_date, slot_type, status)
VALUES (1, 1, CURRENT_DATE + 10, 'MORNING', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO hall_availability_slots (id, hall_id, slot_date, slot_type, status)
VALUES (2, 1, CURRENT_DATE + 10, 'EVENING', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO hall_availability_slots (id, hall_id, slot_date, slot_type, status)
VALUES (3, 2, CURRENT_DATE + 15, 'FULL_DAY', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO hall_availability_slots (id, hall_id, slot_date, slot_type, status)
VALUES (4, 3, CURRENT_DATE + 20, 'EVENING', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('hall_availability_slots', 'id'), (SELECT MAX(id) FROM hall_availability_slots));
