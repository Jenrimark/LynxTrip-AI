package com.lynxtrip.mapper;

import com.lynxtrip.domain.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password, display_name AS displayName, avatar_url AS avatarUrl, gender, phone, balance, identity_status AS identityStatus, created_at AS createdAt, updated_at AS updatedAt
            FROM users
            WHERE id = #{id}
            """)
    UserEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, username, password, display_name AS displayName, avatar_url AS avatarUrl, gender, phone, balance, identity_status AS identityStatus, created_at AS createdAt, updated_at AS updatedAt
            FROM users
            WHERE username = #{account} OR phone = #{account}
            LIMIT 1
            """)
    UserEntity findByAccount(@Param("account") String account);

    @Insert("""
            INSERT INTO users(username, password, display_name, avatar_url, gender, phone, balance, identity_status)
            VALUES(#{u.username}, #{u.password}, #{u.displayName}, #{u.avatarUrl}, #{u.gender}, #{u.phone}, #{u.balance}, #{u.identityStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "u.id")
    int insert(@Param("u") UserEntity u);

    @Update("""
            UPDATE users
            SET password = #{password}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("""
            UPDATE users
            SET display_name = #{u.displayName},
                gender = #{u.gender},
                phone = #{u.phone},
                avatar_url = #{u.avatarUrl}
            WHERE id = #{u.id}
            """)
    int updateProfile(@Param("u") UserEntity u);
}
