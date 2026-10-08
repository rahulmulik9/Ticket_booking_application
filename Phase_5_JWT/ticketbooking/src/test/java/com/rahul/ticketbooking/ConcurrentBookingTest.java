package com.rahul.ticketbooking;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.CreateShowRequest;
import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.Show;
import com.rahul.ticketbooking.exception.SeatNotAvailableException;
import com.rahul.ticketbooking.service.BookingService;
import com.rahul.ticketbooking.service.MovieService;
import com.rahul.ticketbooking.service.SeatService;
import com.rahul.ticketbooking.service.ShowService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev")
@Slf4j
class ConcurrentBookingTest {

    private static final int THREADS = 100;

    @Autowired
    private MovieService movieService;
    @Autowired
    private ShowService showService;
    @Autowired
    private SeatService seatService;
    @Autowired
    private BookingService bookingService;

    @Test
    void hundredUsersBookSameSeat_onlyOneShouldWin() throws Exception {
        // 1. Arrange: one movie, one show, pick one seat
        Movie movie = new Movie();
        movie.setTitle("Concurrency Test Movie");
        movie.setLanguage("English");
        movie.setDurationMinutes(120);
        Movie savedMovie = movieService.createMovie(movie);

        CreateShowRequest showRequest = new CreateShowRequest();
        showRequest.setMovieId(savedMovie.getId());
        showRequest.setStartTime(LocalDateTime.now().plusDays(1));
        showRequest.setPrice(new BigDecimal("250.00"));
        Show show = showService.createShow(showRequest);

        List<Seat> seats = seatService.getSeatsByShow(show.getId());
        Long targetSeatId = seats.get(0).getId();   // seat A1

        // 2. Act: 100 threads wait at the start line, then all fire together
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);   // each thread says "I am ready"
        CountDownLatch start = new CountDownLatch(1);         // main thread says "go"
        CountDownLatch done = new CountDownLatch(THREADS);    // each thread says "I finished"

        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger otherError = new AtomicInteger();

        for (int i = 1; i <= THREADS; i++) {
            long userId = i;
            executor.submit(() -> {
                try {
                    BookingRequest request = new BookingRequest();
                    request.setShowId(show.getId());
                    request.setUserId(userId);
                    request.setSeatIds(List.of(targetSeatId));

                    ready.countDown();
                    start.await();   // wait for the "go" signal

                    bookingService.createBooking(request);
                    success.incrementAndGet();
                } catch (SeatNotAvailableException e) {
                    conflict.incrementAndGet();
                } catch (Exception e) {
                    otherError.incrementAndGet();
                    log.warn("Unexpected error: {}", e.toString());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();        // all 100 threads are standing at the start line
        start.countDown();    // go!
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Report
        log.info("RESULT: success={}, conflict(409)={}, otherError={}",
                success.get(), conflict.get(), otherError.get());

        // 4. Assert: exactly one booking must win (this FAILS today, that is the point)
        assertEquals(1, success.get(), "Seat was booked more than once: double booking!");
    }
}