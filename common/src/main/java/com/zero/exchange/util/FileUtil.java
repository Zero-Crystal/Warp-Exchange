package com.zero.exchange.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static boolean saveStringToLocal(String content, String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            } else {
                file.mkdir();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            log.debug("FileUtil: content save to local failed.");
            e.printStackTrace();
            return false;
        }
    }

    public static String loadLocalTxtFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            log.debug("FileUtil: load local txt file failed.");
            e.printStackTrace();
            return null;
        }
    }
}
