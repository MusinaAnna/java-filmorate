package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public User create(User user) {
        normalizeName(user);
        return userStorage.create(user);
    }

    public User update(User user) {
        normalizeName(user);
        return userStorage.update(user);
    }

    public void delete(Long id) {
        userStorage.delete(id);
    }

    public void addFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        String sql = "MERGE INTO friendship (user_id, friend_id, confirmed) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, true);
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        findById(userId);
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ? " +
                "JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ?";
        return jdbcTemplate.query(sql, new UserDbStorage.UserRowMapper(), userId, otherId);
    }

    private void normalizeName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
