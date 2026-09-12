INSERT INTO vendors (id, owner_name, business_name, category, location, description, base_price, contact_email, contact_phone, verification_status, rating, created_at, updated_at)
VALUES (1, 'Anita Sharma', 'Anita Sharma Photography', 'PHOTOGRAPHY', 'Mumbai', 'Candid wedding photography & cinematography', 45000.00, 'anita@ashaphotography.example', '9876500001', 'VERIFIED', 4.50, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO vendors (id, owner_name, business_name, category, location, description, base_price, contact_email, contact_phone, verification_status, rating, created_at, updated_at)
VALUES (2, 'Rohan Kapoor', 'Royal Feast Caterers', 'CATERING', 'Delhi', 'Multi-cuisine wedding catering for up to 1000 guests', 800.00, 'rohan@royalfeast.example', '9876500002', 'VERIFIED', 4.20, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO vendors (id, owner_name, business_name, category, location, description, base_price, contact_email, contact_phone, verification_status, rating, created_at, updated_at)
VALUES (3, 'Priya Verma', 'Bloom Decor Studio', 'DECORATION', 'Pune', 'Floral & theme-based wedding decoration', 60000.00, 'priya@bloomdecor.example', '9876500003', 'PENDING', 0.00, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO vendors (id, owner_name, business_name, category, location, description, base_price, contact_email, contact_phone, verification_status, rating, created_at, updated_at)
VALUES (4, 'Karan Mehta', 'BeatDrop Entertainment', 'DJ', 'Mumbai', 'DJ, live band and sound setup for wedding events', 30000.00, 'karan@beatdrop.example', '9876500004', 'UNVERIFIED', 0.00, now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('vendors', 'id'), (SELECT MAX(id) FROM vendors));

INSERT INTO portfolio_items (id, vendor_id, title, image_url, description)
VALUES (1, 1, 'Sharma-Reddy Wedding', 'https://example.com/images/portfolio1.jpg', 'Candid coverage of a 3-day wedding')
ON CONFLICT (id) DO NOTHING;

INSERT INTO portfolio_items (id, vendor_id, title, image_url, description)
VALUES (2, 3, 'Royal Palace Decor', 'https://example.com/images/portfolio2.jpg', 'Marigold and fairy-light theme decoration')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('portfolio_items', 'id'), (SELECT MAX(id) FROM portfolio_items));

INSERT INTO commissions (id, vendor_id, booking_id, amount, created_at)
VALUES (1, 1, 1001, 4500.00, now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO commissions (id, vendor_id, booking_id, amount, created_at)
VALUES (2, 2, 1002, 8000.00, now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('commissions', 'id'), (SELECT MAX(id) FROM commissions));
