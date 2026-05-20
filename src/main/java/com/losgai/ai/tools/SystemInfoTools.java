package com.losgai.ai.tools;

import org.springframework.ai.tool.annotation.Tool;

public class SystemInfoTools {

    @Tool(name = "GetSystemProperties", description = "Get current JVM system properties including Java version, OS info, user info, etc.")
    String getSystemProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Java Home: ").append(System.getProperty("java.home")).append("\n");
        sb.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        sb.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("OS Arch: ").append(System.getProperty("os.arch")).append("\n");
        sb.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        sb.append("User Dir: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("File Encoding: ").append(System.getProperty("file.encoding")).append("\n");
        return sb.toString();
    }

    @Tool(name = "GetMemoryInfo", description = "Get current JVM memory usage information in MB")
    String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        return "Max Memory: " + maxMemory + " MB\n"
                + "Total Memory: " + totalMemory + " MB\n"
                + "Free Memory: " + freeMemory + " MB\n"
                + "Used Memory: " + usedMemory + " MB\n"
                + "Available Processors: " + runtime.availableProcessors();
    }

    @Tool(name = "GetCurrentTimestamp", description = "Get current Unix timestamp in seconds and milliseconds")
    String getCurrentTimestamp() {
        long millis = System.currentTimeMillis();
        return "Timestamp (ms): " + millis + "\n"
                + "Timestamp (s): " + (millis / 1000);
    }

    @Tool(name = "GetDiskInfo", description = "Get disk space information for the root partition")
    String getDiskInfo() {
        java.io.File root = new java.io.File("/");
        long totalSpace = root.getTotalSpace() / (1024 * 1024 * 1024);
        long freeSpace = root.getFreeSpace() / (1024 * 1024 * 1024);
        long usableSpace = root.getUsableSpace() / (1024 * 1024 * 1024);

        return "Total Space: " + totalSpace + " GB\n"
                + "Free Space: " + freeSpace + " GB\n"
                + "Usable Space: " + usableSpace + " GB";
    }
}
