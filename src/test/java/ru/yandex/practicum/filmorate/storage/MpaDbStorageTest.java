package ru.yandex.practicum.filmorate.storage.mpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.util.Collection;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(MpaDbStorage.class)
@Sql(scripts = {"/schema.sql", "/data.sql"})
class MpaDbStorageTest {

    @Autowired
    private MpaDbStorage mpaStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFindAllMpa() {
        Collection<MpaRating> mpaList = mpaStorage.findAll();
        assertThat(mpaList).hasSize(5);
        assertThat(mpaList).extracting(MpaRating::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void testFindMpaById() {
        Optional<MpaRating> mpa = mpaStorage.findById(1L);
        assertThat(mpa).isPresent();
        assertThat(mpa.get().getName()).isEqualTo("G");
    }

    @Test
    void testFindMpaByIdNotFound() {
        Optional<MpaRating> mpa = mpaStorage.findById(999L);
        assertThat(mpa).isEmpty();
    }
}
