package com.zero.exchange.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ClassPathUtil {

    public static String readFile(String classPath) throws IOException {
        try(InputStream inputStream = ClassPathUtil.class.getResourceAsStream(classPath)) {
            if (inputStream == null) {
                throw new IOException("class path [" + classPath + "] not found");
            }
            return readString(inputStream);
        }
    }

    public static String readString(InputStream inputStream) throws IOException {
        return new String(readBytes(inputStream), StandardCharsets.UTF_8);
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024 * 1024);
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }
}
