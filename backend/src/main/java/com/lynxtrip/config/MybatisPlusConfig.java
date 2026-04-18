package com.lynxtrip.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    /**
     * 在受限环境（如沙箱/容器）里 MyBatis-Plus 默认会尝试读取网卡信息推导雪花算法参数，
     * 可能触发 getifaddrs 权限错误。这里固定 workerId/datacenterId 避免启动时报错刷屏。
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new DefaultIdentifierGenerator(1L, 1L);
    }
}

