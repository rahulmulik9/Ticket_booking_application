BEGIN;

-- 100,000 movies (titles are unique and word-based, so the name search works well later)
INSERT INTO movies (title, language, duration_minutes)
SELECT
    (ARRAY['Dark','Last','Silent','Golden','Lost','Hidden','Broken','Eternal','Wild','Midnight'])[1 + n % 10]
    || ' ' ||
    (ARRAY['Knight','Journey','Empire','Legacy','Storm','River','Dream','Code','Horizon','Shadow'])[1 + (n / 10) % 10]
    || ' ' || n,
    (ARRAY['English','Hindi','Telugu','Tamil','Marathi','Malayalam'])[1 + n % 6],
    90 + n % 90
FROM generate_series(1, 100000) AS n;

-- 2 shows per new movie = 200,000 shows
INSERT INTO shows (movie_id, start_time, price)
SELECT m.id,
       date_trunc('day', now()) + (m.id % 30) * interval '1 day' + s * interval '4 hours',
       150 + (m.id % 5) * 50
FROM movies m
CROSS JOIN generate_series(1, 2) AS s
WHERE m.id > 4;

-- 50 seats (A1..E10) for the first 2,000 new shows = 100,000 seats
INSERT INTO seats (show_id, seat_number, status)
SELECT sh.id, r.row_letter || c.num, 'AVAILABLE'
FROM (SELECT id FROM shows WHERE movie_id > 4 ORDER BY id LIMIT 2000) sh
CROSS JOIN unnest(ARRAY['A','B','C','D','E']) AS r(row_letter)
CROSS JOIN generate_series(1, 10) AS c(num);

COMMIT;

-- refresh statistics so the query planner knows the new table sizes
ANALYZE movies;
ANALYZE shows;
ANALYZE seats;