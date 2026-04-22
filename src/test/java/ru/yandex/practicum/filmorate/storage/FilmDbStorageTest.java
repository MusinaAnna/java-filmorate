package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

@JdbcTest
@Import(FilmDbStorage.class)   // явно импортируем наш DAO
@Sql(scripts = {"/schema.sql", "/data.sql"})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;   // нужен для работы DAO, подставится автоматически

    @Test
    void testCreateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Film");
        assertThat(created.getMpa().getId()).isEqualTo(1);
        assertThat(created.getGenres()).hasSize(2);
        assertThat(created.getGenres().get(0).getId()).isEqualTo(1L);
        assertThat(created.getGenres().get(1).getId()).isEqualTo(2L);
    }

    @Test
    void testFindFilmById() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);
        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Film");
    }

    @Test
    void testFindAllFilms() {
        Film film1 = createTestFilm();
        Film film2 = createTestFilm();
        film2.setName("Another Film");
        filmStorage.create(film1);
        filmStorage.create(film2);
        Collection<Film> films = filmStorage.findAll();
        assertThat(films).hasSize(2);
    }

    @Test
    void testUpdateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);
        created.setName("Updated Name");
        filmStorage.update(created);
        Optional<Film> updated = filmStorage.findById(created.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    void testUpdateNotFound() {
        Film film = createTestFilm();
        film.setId(999L);
        assertThatThrownBy(() -> filmStorage.update(film))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void testDeleteFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);
        filmStorage.delete(created.getId());
        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isEmpty();
    }

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        film.setMpa(mpa);
        List<Genre> genres = new ArrayList<>();
        Genre genre1 = new Genre();
        genre1.setId(1L);
        Genre genre2 = new Genre();
        genre2.setId(2L);
        genres.add(genre1);
        genres.add(genre2);
        film.setGenres(genres);
        return film;
    }
}
