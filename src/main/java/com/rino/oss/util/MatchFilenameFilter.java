package com.rino.oss.util;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * @author zip
 */
public class MatchFilenameFilter implements FilenameFilter {
    private String pattern;

    public MatchFilenameFilter(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean accept(File dir, String name) {
        return StringUtils.isEmpty(pattern) ? true : Pattern.matches(pattern, name);
    }
}
