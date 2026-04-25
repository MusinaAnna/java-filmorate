package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Film> findAll() {
        String sql = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
            FROM film f
            LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id
        """;
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        loadGenresForFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
            FROM film f
            LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id
            WHERE f.id = ?
        """;
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        loadGenresForFilm(film);
        return Optional.of(film);
    }

    @Override
    public Film create(Film film) {
        String sql = """
            INSERT INTO film (name, description, release_date, duration, mpa_rating_id)
            VALUES (?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        film.setId(id);
        updateGenres(film);
        loadGenresForFilm(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = """
            UPDATE film
            SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ?
            WHERE id = ?
        """;
        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                java.sql.Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());
        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        updateGenres(film);
        loadGenresForFilm(film);
        return film;
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM film_genre WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ?", id);
        jdbcTemplate.update("DELETE FROM film WHERE id = ?", id);
    }

    @Override
    public List<Film> getPopularFilms(int limit) {
        String sql = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name,
                   COUNT(fl.user_id) AS likes_count
            FROM film f
            LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            GROUP BY f.id
            ORDER BY likes_count DESC
            LIMIT ?
        """;
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), limit);
        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }
        return films;
    }


    private void updateGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genre WHERE film_id = ?", film.getId());
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : film.getGenres()) {
            if (genre.getId() != null) {
                jdbcTemplate.update(sql, film.getId(), genre.getId());
            }
        }
    }

    private void loadGenresForFilms(Collection<Film> films) {
        if (films == null || films.isEmpty()) return;

        String ids = films.stream()
                .map(f -> String.valueOf(f.getId()))
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM film_genre fg " +
                "JOIN genre g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + ids + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Long, List<Genre>> genreMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getLong("id"));
            genre.setName(rs.getString("name"));
            genreMap.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        });

        for (Film film : films) {
            List<Genre> genres = genreMap.getOrDefault(film.getId(), new ArrayList<>());
            film.setGenres(genres);
        }
    }

    private void loadGenresForFilm(Film film) {
        String sql = """
            SELECT g.id, g.name
            FROM film_genre fg
            JOIN genre g ON fg.genre_id = g.id
            WHERE fg.film_id = ?
            ORDER BY g.id
        """;
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getLong("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, film.getId());
        film.setGenres(genres);
    }

    static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getLong("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));

            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);
            return film;
        }
    }
}
