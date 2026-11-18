package org.example.backend.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

/**
 * 账号 Mapper（成员 1 scaffold）。
 */
@Mapper
public interface UserAccountMapper {

    @Select("SELECT * FROM user_account WHERE username = #{username}")
    Optional<UserAccount> selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_account WHERE id = #{id}")
    Optional<UserAccount> selectById(@Param("id") Long id);

    @Select("SELECT status FROM user_account WHERE id = #{id}")
    String selectStatusById(@Param("id") Long id);

    @Insert("INSERT INTO user_account (username, password_hash, display_name, role, status) "
            + "VALUES (#{username}, #{passwordHash}, #{displayName}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount account);

    @Update("UPDATE user_account SET status = #{status}, updated_at = now() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
