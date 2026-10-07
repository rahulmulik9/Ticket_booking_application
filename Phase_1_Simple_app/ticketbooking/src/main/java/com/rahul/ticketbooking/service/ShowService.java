package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.CreateShowRequest;
import com.rahul.ticketbooking.entity.Movie;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.Show;
import com.rahul.ticketbooking.repository.SeatRepository;
import com.rahul.ticketbooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowService {

    private static final String[] ROWS = {"A", "B", "C", "D", "E"};
    private static final int SEATS_PER_ROW = 10;

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final MovieService movieService;

    public Show createShow(CreateShowRequest request) {
        Movie movie = movieService.getMovieById(request.getMovieId());

        Show show = new Show();
        show.setMovie(movie);
        show.setStartTime(request.getStartTime());
        show.setPrice(request.getPrice());
        Show savedShow = showRepository.save(show);

        seatRepository.saveAll(generateSeats(savedShow));
        log.info("Created show {} with {} seats", savedShow.getId(), ROWS.length * SEATS_PER_ROW);
        return savedShow;
    }

    public List<Show> getShowsByMovie(Long movieId) {
        movieService.getMovieById(movieId);   // fails if the movie does not exist
        return showRepository.findByMovieId(movieId);
    }

    public Show getShowById(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found with id " + id));
    }

    private List<Seat> generateSeats(Show show) {
        List<Seat> seats = new ArrayList<>();
        for (String row : ROWS) {
            for (int number = 1; number <= SEATS_PER_ROW; number++) {
                Seat seat = new Seat();
                seat.setShow(show);
                seat.setSeatNumber(row + number);
                seats.add(seat);
            }
        }
        return seats;
    }
}