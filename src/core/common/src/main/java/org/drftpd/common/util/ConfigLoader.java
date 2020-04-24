package org.drftpd.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class ConfigLoader {

    public static String configPath(String confDirectory) {
        String configPath = System.getenv("DRFTPD_CONFIG_PATH");
        return configPath != null ? (configPath + "/" + confDirectory) : confDirectory;
    }

    public static BufferedReader loadTextFile(String fileName) throws IOException {
        String filePathName = configPath("config/themes/text/" + fileName);
        File textFile = new File(filePathName);
        if (textFile.exists()) return Files.newBufferedReader(textFile.toPath());
        File distFile = new File(filePathName + ".dist");
        if (!distFile.exists()) throw new RuntimeException("Cant find text file " + filePathName + ".dist");
        return Files.newBufferedReader(distFile.toPath());
    }

    public static File loadConfigFile(String fileName, boolean isPlugin) {
        String filePathName = (isPlugin ? "config/plugins/" : "config/") + fileName;
        String fullPath = configPath(filePathName);
        File conf = new File(fullPath);
        if (conf.exists()) return conf;
        File distFile = new File(fullPath + ".dist");
        if (!distFile.exists()) throw new RuntimeException("Cant find config file " + filePathName + ".dist");
        return distFile;

    }

    public static Properties loadPropertyConfig(String fileName, boolean isPlugin) {
        File configFile = loadConfigFile(fileName, isPlugin);
        Properties p = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configFile);
            p.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore fail on closure
                }
            }
        }
        return p;
    }

    public static Properties loadConfig(String fileName) {
        return loadPropertyConfig(fileName, false);
    }

    public static Properties loadPluginConfig(String fileName) {
        return loadPropertyConfig(fileName, true);
    }
}
