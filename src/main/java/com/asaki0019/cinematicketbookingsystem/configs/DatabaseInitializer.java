package com.asaki0019.cinematicketbookingsystem.configs;

import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        LogSystem.info("开始检查数据库完整性...");

        try {
            // 检查外键约束是否存在
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet foreignKeys = metaData.getImportedKeys(null, null, "session");

                if (foreignKeys.next()) {
                    // 如果能找到外键，说明数据库结构已经完整
                    LogSystem.info("数据库结构已存在且完整，跳过初始化。");
                    return;
                }
            }

            // 如果没有找到外键约束，执行完整的初始化脚本
            LogSystem.warn("检测到数据库结构不完整，将执行初始化脚本 `DatabaseSQL.sql`...");
            try {
                Resource resource = new ClassPathResource("DatabaseSQL.sql");
                try (Connection connection = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(connection, resource);
                    LogSystem.info("数据库初始化脚本执行成功。");
                }
            } catch (Exception ex) {
                LogSystem.error("数据库初始化脚本执行失败: " + ex.getMessage());
                throw new RuntimeException("数据库初始化失败，应用启动中止。", ex);
            }
        } catch (Exception e) {
            LogSystem.error("数据库检查过程发生错误: " + e.getMessage());
            throw new RuntimeException("数据库检查失败，应用启动中止。", e);
        }
    }
}