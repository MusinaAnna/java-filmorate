package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name " +
                "FROM film f LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        for (Film film : films) {
            loadGenres(film);
        }
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name " +
                "FROM film f LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id WHERE f.id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, new FilmRowMapper(), id);
            if (film != null) {
                loadGenres(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO film (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setObject(3, film.getReleaseDate());
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        updateGenres(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE film SET name=?, description=?, release_date=?, duration=?, mpa_rating_id=? WHERE id=?";
        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());
        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        updateGenres(film);
        return film;
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM film WHERE id=?", id);
    }

    @Override
    public void loadGenres(Film film) {
        String sql = "SELECT g.id, g.name FROM film_genre fg " +
                "JOIN genre g ON fg.genre_id = g.id WHERE fg.film_id = ? ORDER BY fg.genre_order";
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getLong("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, film.getId());
        film.setGenres(genres);
        log.debug("Загружено {} жанров для фильма id={}", genres.size(), film.getId());
    }

    private void updateGenres(Film film) {
        log.debug("Обновление жанров для фильма id={}: {}", film.getId(), film.getGenres());
        jdbcTemplate.update("DELETE FROM film_genre WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genre (film_id, genre_id, genre_order) VALUES (?, ?, ?)";
            List<Object[]> batchArgs = new ArrayList<>();
            int order = 0;
            for (Genre genre : film.getGenres()) {
                batchArgs.add(new Object[]{film.getId(), genre.getId(), order++});
            }
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Вставлено {} жанров для фильма id={}", batchArgs.size(), film.getId());
        }
    }

    // Публичный статический RowMapper для переиспользования
    public static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getLong("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date") != null ? rs.getDate("release_date").toLocalDate() : null);
            film.setDuration(rs.getInt("duration"));

            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);
            return film;
        }
    }
}
