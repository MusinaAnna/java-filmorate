package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<MpaRating> findAll() {
        return jdbcTemplate.query("SELECT * FROM mpa_rating ORDER BY id", new MpaRowMapper());
    }

    @Override
    public Optional<MpaRating> findById(Long id) {
        try {
            MpaRating mpa = jdbcTemplate.queryForObject("SELECT * FROM mpa_rating WHERE id = ?", new MpaRowMapper(), id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static class MpaRowMapper implements RowMapper<MpaRating> {
        @Override
        public MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        }
    }
}
