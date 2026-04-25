package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

@JdbcTest
@Import(UserDbStorage.class)
@Sql(scripts = {"/schema.sql", "/data.sql"})
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateUser() {
        User user = createTestUser();
        User created = userStorage.create(user);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testFindUserById() {
        User user = createTestUser();
        User created = userStorage.create(user);
        Optional<User> found = userStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLogin()).isEqualTo("testlogin");
    }

    @Test
    void testFindAllUsers() {
        userStorage.create(createTestUser());
        User user2 = createTestUser();
        user2.setEmail("another@example.com");
        user2.setLogin("anotherlogin");
        userStorage.create(user2);
        Collection<User> users = userStorage.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    void testUpdateUser() {
        User user = createTestUser();
        User created = userStorage.create(user);
        created.setName("New Name");
        userStorage.update(created);
        Optional<User> updated = userStorage.findById(created.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("New Name");
    }

    @Test
    void testUpdateNotFound() {
        User user = createTestUser();
        user.setId(999L);
        assertThatThrownBy(() -> userStorage.update(user))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void testDeleteUser() {
        User user = createTestUser();
        User created = userStorage.create(user);
        userStorage.delete(created.getId());
        Optional<User> found = userStorage.findById(created.getId());
        assertThat(found).isEmpty();
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}
