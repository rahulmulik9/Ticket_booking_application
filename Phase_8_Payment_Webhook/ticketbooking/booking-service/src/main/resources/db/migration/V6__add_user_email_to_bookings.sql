-- nullable: bookings created before this step have no email
ALTER TABLE bookings ADD COLUMN user_email VARCHAR(255);