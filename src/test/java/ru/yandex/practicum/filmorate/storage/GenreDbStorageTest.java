package ru.yandex.practicum.filmorate.storage.genre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.Collection;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(GenreDbStorage.class)
@Sql(scripts = {"/schema.sql", "/data.sql"})
class GenreDbStorageTest {

    @Autowired
    private GenreDbStorage genreStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFindAllGenres() {
        Collection<Genre> genres = genreStorage.findAll();
        assertThat(genres).hasSize(6);
        assertThat(genres).extracting(Genre::getName)
                .contains("Комедия", "Драма", "Мультфильм");
    }

    @Test
    void testFindGenreById() {
        Optional<Genre> genre = genreStorage.findById(1L);
        assertThat(genre).isPresent();
        assertThat(genre.get().getName()).isEqualTo("Комедия");
    }

    @Test
    void testFindGenreByIdNotFound() {
        Optional<Genre> genre = genreStorage.findById(999L);
        assertThat(genre).isEmpty();
    }
}
