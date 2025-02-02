package com.techelevator.dao;

import com.techelevator.exception.DaoException;
import com.techelevator.model.Attraction;
import com.techelevator.model.Badge;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcBadgeDao implements BadgeDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBadgeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
@Override
    public Badge getBadgeById(int id){
        Badge badge = null;
        String sql = "SELECT badge_id, name, description, locked_image, unlocked_image, type_id, unlocked" +
                " FROM badge WHERE badge_id = ?";

        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, id);
            if (results.next()) {
               badge = mapRowToBadge(results);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return badge;
    }
    @Override
    public List<Badge> getUserBadges(int id) {
        List<Badge> badge = new ArrayList<>();
        //TODO: DONE - SQL Incorrectly retrieving all badges instead of User Badges
        /*
        String sql = "SELECT badge_id, name, description, locked_image, unlocked_image, type_id, unlocked" +
                        " FROM badge ORDER BY name ASC";
        */
        //TODO: REFACTOR BASE QUERY INTO VIEW !!WATCH THE USER_ID PARAM PLACEMENT, MOVE TO WHERE CLAUSE
        String sql = "SELECT DISTINCT b.badge_id, name, description, locked_image, unlocked_image, type_id, (ub.badge_id IS NOT NULL) as unlocked \n" +
                "FROM badge b\n" +
                "\tLEFT OUTER JOIN user_badge ub \n" +
                "\t\tON ub.badge_id = b.badge_id\n" +
                "\t\t\tAND\n" +
                "\t\t\tub.user_id = ?\n" +
                "ORDER BY name ASC;";

        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, id);
            while (results.next()) {
                Badge badges = mapRowToBadge(results);
                badge.add(badges);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return badge;
    }
    @Override
    public Badge getBadgeByName(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Badge badge = null;

        String sql = "SELECT badge_id, name, description, locked_image, unlocked_image, type_id, unlocked" +
                " FROM badge WHERE name = ?";
        try {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, name);
            if (rowSet.next()) {
                badge = mapRowToBadge(rowSet);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return badge;
    }

    @Override
    public Badge getBadgeByTypeId(int typeId){
        Badge badge = null;
        String sql = "SELECT badge_id, name, description, locked_image, unlocked_image, type_id, unlocked" +
                " FROM badge WHERE type_id = ?";

        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, typeId);
            if (results.next()) {
                badge = mapRowToBadge(results);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return badge;

    }

    @Override
    public String getBadgeNameByCheckIn(int Id){
        String badgeName = null;
        String sql = "SELECT b.name" +
                " FROM badge b JOIN type t ON b.type_id = t.type_id JOIN attraction a ON t.type_id = a.type_id " +
                "JOIN checkin c ON a.attraction_id = c.attraction_id WHERE c.attraction_id = ?";

        try {
            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, Id);
            if (results.next()) {
                badgeName = results.getString("name");
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
        return badgeName;

    }
    @Override
    public Badge createBadge(Badge badge) {
        Badge newBadge = null;
        String insertBadgeSql = "INSERT INTO badge ( " +
                " name, description) " +
                " VALUES ( ?, ?)" +
                " RETURNING badge_id";

        try {
            int newBadgeId = jdbcTemplate.queryForObject(insertBadgeSql, int.class, badge.getName(), badge.getDescription());
            newBadge = getBadgeById(newBadgeId);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation", e);
        }
        return newBadge;
    }
    @Override
    public int deleteBadgeById(int id){
        int numberOfRows = 0;
        String badgeDeleteSql = "DELETE FROM badge WHERE badge_id = ?";
        try {
            numberOfRows = jdbcTemplate.update(badgeDeleteSql, id);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation", e);
        }
        return numberOfRows;

    }
    @Override
    public Badge updateBadge(Badge badge) {
        Badge updatedBadge = null;
        String sql = "UPDATE badge SET name=?, description=?, locked_image=?, unlocked_image=?, type_id=?, unlocked=true  WHERE badge_id=?";
        try {
            int rowsAffected = jdbcTemplate.update(sql, badge.getName(), badge.getDescription(), badge.getLockedImage(),badge.getUnlockedImage(), badge.getTypeId(), badge.getId()); //TODO:UPDATE IS BREAKING RIGHT HERE

            if (rowsAffected == 0) {
                throw new DaoException("Zero rows affected, expected at least one");
            } else {
                updatedBadge = getBadgeById(badge.getId());
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (DataIntegrityViolationException e) {
            throw new DaoException("Data integrity violation", e);
        }

        return updatedBadge;
    }

    private Badge mapRowToBadge(SqlRowSet rs) {
        Badge badge = new Badge();
        badge.setId(rs.getInt("badge_id"));
        badge.setName(rs.getString("name"));
        badge.setDescription(rs.getString("description"));
        badge.setLockedImage(rs.getString("locked_image"));
        badge.setUnlockedImage(rs.getString("unlocked_image"));
        badge.setTypeId(rs.getInt("type_id"));
        badge.setUnlocked(rs.getBoolean("unlocked"));
        return badge;
    }




}
