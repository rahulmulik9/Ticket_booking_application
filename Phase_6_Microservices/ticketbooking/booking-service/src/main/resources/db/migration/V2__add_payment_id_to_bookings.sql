-- plain id of the payment, which lives in payment-service. No foreign key.
ALTER TABLE bookings ADD COLUMN payment_id BIGINT;