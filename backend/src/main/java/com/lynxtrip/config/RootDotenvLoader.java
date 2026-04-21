package com.lynxtrip.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在 Spring 启动前加载仓库根目录下的 {@code .env}，写入 {@link System#setProperty}，
 * 供 {@code application.yml} / {@code application-mysql.yml} 中的 {@code ${VAR}} 使用。
 * <p>
 * 已存在于操作系统环境变量中的键不会被覆盖（与常见 dotenv 约定一致）。
 * <p>
 * 查找顺序：环境变量 {@code LYNXTRIP_DOTENV_FILE} 指向的文件 → {@code user.dir/.env} →
 * {@code user.dir/../.env}（便于从 {@code backend/} 目录启动时仍读到仓库根目录配置）。
 * <p>
 * 若未找到可用的 .env 文件（或 {@code LYNXTRIP_DOTENV_FILE} 指向不存在的路径），将抛出
 * {@link IllegalStateException}，拒绝启动，避免静默沿用 application*.yml 默认值。
 */
public final class RootDotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(RootDotenvLoader.class);

    private RootDotenvLoader() {}

    public static void load() {
        String explicit = System.getenv("LYNXTRIP_DOTENV_FILE");
        if (explicit != null && !explicit.isBlank()) {
            Path p = Paths.get(explicit).toAbsolutePath().normalize();
            if (!Files.isRegularFile(p)) {
                String msg = String.format(
                        "[LynxTrip] 已设置 LYNXTRIP_DOTENV_FILE，但文件不存在: %s。请创建该文件，或取消该环境变量以使用项目根目录的 .env。",
                        p);
                log.error(msg);
                throw new IllegalStateException(msg);
            }
            applyDotenv(p);
            return;
        }

        Path envFile = resolveStandardEnvFile();
        if (envFile == null) {
            String msg =
                    "[LynxTrip] 未找到 .env，后端已拒绝启动。请在仓库根目录执行: cp .env.example .env，再按需填写数据库与密钥等配置；"
                            + "或设置环境变量 LYNXTRIP_DOTENV_FILE 指向你的 .env 绝对路径。";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        applyDotenv(envFile);
    }

    private static void applyDotenv(Path envFile) {
        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .load();
        dotenv.entries().forEach(e -> {
            String key = e.getKey();
            if (key == null || key.isBlank()) {
                return;
            }
            String val = e.getValue();
            if (val == null) {
                return;
            }

            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, val);
            }

            String canonicalKey = toCanonicalPropertyKey(key);
            if (canonicalKey != null
                    && System.getenv(canonicalKey) == null
                    && System.getProperty(canonicalKey) == null) {
                System.setProperty(canonicalKey, val);
            }
        });
    }

    private static String toCanonicalPropertyKey(String key) {
        String normalized = key.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        // Allow .env keys like SPRING_PROFILES_ACTIVE to map to spring.profiles.active.
        return normalized.replace('_', '.');
    }

    private static Path resolveStandardEnvFile() {
        String userDir = System.getProperty("user.dir");
        if (userDir == null) {
            return null;
        }
        Path here = Paths.get(userDir, ".env").toAbsolutePath().normalize();
        if (Files.isRegularFile(here)) {
            return here;
        }
        Path parent = Paths.get(userDir, "..", ".env").toAbsolutePath().normalize();
        if (Files.isRegularFile(parent)) {
            return parent;
        }
        return null;
    }
}
