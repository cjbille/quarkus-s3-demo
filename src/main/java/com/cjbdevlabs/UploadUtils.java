package com.cjbdevlabs;

import java.util.UUID;

public class UploadUtils {

    public static String buildFileName(String fileNameHeader) {
        var fileName = isNotBlank(fileNameHeader) ? fileNameHeader : UUID.randomUUID().toString();
        return fileName + ".tar";
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
