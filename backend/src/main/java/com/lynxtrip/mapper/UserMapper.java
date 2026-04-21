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
            FROM yonghu
            WHERE id = #{id}
            """)
    UserEntity findById(@Param("id") Long id);

    @Select("""
            SELECT id, yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng, addtime, updated_at AS updatedAt
            FROM yonghu
            WHERE yonghuming = #{account} OR lianxidianhua = #{account}
            LIMIT 1
            """)
    UserEntity findByAccount(@Param("account") String account);

    @Insert("""
            INSERT INTO yonghu(yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng)
            VALUES(#{u.yonghuming}, #{u.mima}, #{u.xingming}, #{u.touxiang}, #{u.xingbie}, #{u.lianxidianhua}, #{u.money}, #{u.shimingrenzheng})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "u.id")
    int insert(@Param("u") UserEntity u);

    @Update("""
            UPDATE yonghu
            SET mima = #{password}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
