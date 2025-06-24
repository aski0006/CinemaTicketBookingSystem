package com.asaki0019.cinematicketbookingsystem.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LogSystem {
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    private static LogLevel currentLevel = LogLevel.INFO;
    private static String logFilePath = "logs/app.log";
    private static boolean logToConsole = true;
    private static int maxFileSizeMB = 5;
    private static int maxBackupFiles = 3;
    private static boolean async = true;
    private static BufferedWriter fileWriter = null;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final LinkedBlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static volatile boolean running = true;

    static {
        try {
            Properties props = new Properties();
            props.load(LogSystem.class.getClassLoader().getResourceAsStream("application.properties"));
            String filePath = props.getProperty("logsystem.logfile");
            if (filePath != null && !filePath.isEmpty()) {
                logFilePath = filePath;
            }
            String console = props.getProperty("logsystem.console");
            if (console != null) {
                logToConsole = Boolean.parseBoolean(console);
            }
            String maxSize = props.getProperty("logsystem.max_file_size_mb");
            if (maxSize != null) {
                maxFileSizeMB = Integer.parseInt(maxSize);
            }
            String maxBackup = props.getProperty("logsystem.max_backup_files");
            if (maxBackup != null) {
                maxBackupFiles = Integer.parseInt(maxBackup);
            }
            String asyncStr = props.getProperty("logsystem.async");
            if (asyncStr != null) {
                async = Boolean.parseBoolean(asyncStr);
            }
            File logFile = new File(logFilePath);
            logFile.getParentFile().mkdirs();
            fileWriter = new BufferedWriter(new FileWriter(logFile, true));
            if (async) {
                startAsyncWriter();
            }
        } catch (Exception e) {
            System.err.println("[LogSystem] 日志系统初始化失败: " + e.getMessage());
        }
    }

    private static void startAsyncWriter() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("LogSystem-AsyncWriter-" + count.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        factory.newThread(() -> {
            while (running) {
                try {
                    String logMsg = logQueue.take();
                    writeLogToFile(logMsg);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    public static void setLogLevel(LogLevel level) {
        currentLevel = level;
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    public static void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }

    private static void log(LogLevel level, String message) {
        if (level.ordinal() >= currentLevel.ordinal()) {
            String time = sdf.format(new Date());
            String logMsg = String.format("[%s] [%s] %s", time, level.name(), message);
            if (logToConsole) {
                System.out.println(logMsg);
            }
            if (async) {
                logQueue.offer(logMsg);
            } else {
                writeLogToFile(logMsg);
            }
        }
    }

    private static synchronized void writeLogToFile(String logMsg) {
        try {
            rotateLogFileIfNeeded();
            if (fileWriter != null) {
                fileWriter.write(logMsg);
                fileWriter.newLine();
                fileWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[LogSystem] 写入日志文件失败: " + e.getMessage());
        }
    }

    private static void rotateLogFileIfNeeded() throws IOException {
        File logFile = new File(logFilePath);
        if (logFile.length() >= maxFileSizeMB * 1024 * 1024) {
            if (fileWriter != null) {
                fileWriter.close();
            }
            for (int i = maxBackupFiles - 1; i >= 1; i--) {
                File src = new File(logFilePath + "." + i);
                File dest = new File(logFilePath + "." + (i + 1));
                if (src.exists()) {
                    src.renameTo(dest);
                }
            }
            File firstBackup = new File(logFilePath + ".1");
            logFile.renameTo(firstBackup);
            fileWriter = new BufferedWriter(new FileWriter(logFilePath, false));
        }
    }

    public static void shutdown() {
        running = false;
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException ignored) {
            }
        }
    }

    // 便捷日志方法，供AOP调用
    public static void log(String message) {
        info(message);
    }

    public static void logLevel(LogLevel level, String message) {
        log(level, message);
    }
}