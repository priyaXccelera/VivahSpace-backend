INSERT INTO events (id, proposal_id, hall_id, user_id, guest_count, status, created_at, updated_at)
VALUES (1, 1, 1, 2, 250, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO events (id, proposal_id, hall_id, user_id, guest_count, status, created_at, updated_at)
VALUES (2, 2, 3, 3, 400, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT MAX(id) FROM events));

INSERT INTO rsvp_entries (id, event_id, guest_name, contact_info, status)
VALUES (1, 1, 'Neha Gupta', 'neha@example.com', 'CONFIRMED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO rsvp_entries (id, event_id, guest_name, contact_info, status)
VALUES (2, 1, 'Sameer Joshi', 'sameer@example.com', 'PENDING')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('rsvp_entries', 'id'), (SELECT MAX(id) FROM rsvp_entries));

INSERT INTO checklist_items (id, event_id, title, done, due_date)
VALUES (1, 1, 'Confirm catering headcount', false, CURRENT_DATE + 5)
ON CONFLICT (id) DO NOTHING;

INSERT INTO checklist_items (id, event_id, title, done, due_date)
VALUES (2, 1, 'Finalize decoration theme', true, CURRENT_DATE + 2)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('checklist_items', 'id'), (SELECT MAX(id) FROM checklist_items));

INSERT INTO itinerary_entries (id, event_id, title, start_time, end_time, description)
VALUES (1, 1, 'Baraat Arrival', '17:00:00', '17:30:00', 'Groom procession arrives at the venue')
ON CONFLICT (id) DO NOTHING;

INSERT INTO itinerary_entries (id, event_id, title, start_time, end_time, description)
VALUES (2, 1, 'Dinner', '20:00:00', '22:00:00', 'Multi-cuisine buffet dinner')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('itinerary_entries', 'id'), (SELECT MAX(id) FROM itinerary_entries));
