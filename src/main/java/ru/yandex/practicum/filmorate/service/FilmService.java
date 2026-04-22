package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;
    private final JdbcTemplate jdbcTemplate;

    public Collection<Film> findAll() {
        Collection<Film> films = filmStorage.findAll();
        for (Film film : films) {
            filmStorage.loadGenres(film);
        }
        return films;
    }

    public Film findById(Long id) {
        Film film = filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
        filmStorage.loadGenres(film);
        return film;
    }

    public Film create(Film film) {
        deduplicateGenres(film);
        validateMpaAndGenres(film);
        Film created = filmStorage.create(film);
        filmStorage.loadGenres(created);
        return created;
    }

    public Film update(Film film) {
        deduplicateGenres(film);
        validateMpaAndGenres(film);
        Film updated = filmStorage.update(film);
        filmStorage.loadGenres(updated);
        return updated;
    }

    public void delete(Long id) {
        filmStorage.delete(id);
    }

    public void addLike(Long filmId, Long userId) {
        findById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        String sql = "MERGE INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        int deleted = jdbcTemplate.update(sql, filmId, userId);
        if (deleted > 0) {
            log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
        } else {
            log.warn("Пользователь {} не лайкал фильм {}", userId, filmId);
        }
    }

    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name " +
                "FROM film f " +
                "LEFT JOIN mpa_rating m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id " +
                "ORDER BY COUNT(fl.user_id) DESC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, new FilmDbStorage.FilmRowMapper(), count);
        for (Film film : films) {
            filmStorage.loadGenres(film);
        }
        return films;
    }

    // Удаление дубликатов жанров
    private void deduplicateGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> unique = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            for (Genre g : film.getGenres()) {
                if (g.getId() != null && !seen.contains(g.getId())) {
                    seen.add(g.getId());
                    unique.add(g);
                }
            }
            film.setGenres(unique);
            if (unique.size() < film.getGenres().size()) {
                log.warn("Обнаружены дубликаты жанров, удалены лишние");
            }
        }
    }

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("MPA рейтинг должен быть указан");
        }
        mpaStorage.findById(film.getMpa().getId())
                .orElseThrow(() -> new NotFoundException("MPA рейтинг с id=" + film.getMpa().getId() + " не найден"));

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() == null) {
                    throw new ValidationException("ID жанра не может быть null");
                }
                genreStorage.findById(genre.getId())
                        .orElseThrow(() -> new NotFoundException("Жанр с id=" + genre.getId() + " не найден"));
            }
        }
    }
}
