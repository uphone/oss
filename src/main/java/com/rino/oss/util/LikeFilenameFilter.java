package com.rino.oss.util;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author zip
 */
public class LikeFilenameFilter implements FilenameFilter {
    private String fileName;

    public LikeFilenameFilter(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean accept(File dir, String name) {
        return StringUtils.isEmpty(fileName) ? true : name.indexOf(fileName) != -1;
    }
}
