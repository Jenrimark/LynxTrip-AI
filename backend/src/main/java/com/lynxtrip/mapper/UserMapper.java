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
            SELECT id, yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng, addtime, updated_at AS updatedAt
            FROM users
            WHERE id = #{id}
            """)
    UserEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng, addtime, updated_at AS updatedAt
            FROM users
            WHERE yonghuming = #{account} OR lianxidianhua = #{account}
            LIMIT 1
            """)
    UserEntity findByAccount(@Param("account") String account);

    @Insert("""
            INSERT INTO users(yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng)
            VALUES(#{u.yonghuming}, #{u.mima}, #{u.xingming}, #{u.touxiang}, #{u.xingbie}, #{u.lianxidianhua}, #{u.money}, #{u.shimingrenzheng})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "u.id")
    int insert(@Param("u") UserEntity u);

    @Update("""
            UPDATE users
            SET mima = #{password}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("""
            UPDATE users
            SET xingming = #{u.xingming},
                xingbie = #{u.xingbie},
                lianxidianhua = #{u.lianxidianhua},
                touxiang = #{u.touxiang}
            WHERE id = #{u.id}
            """)
    int updateProfile(@Param("u") UserEntity u);
}
