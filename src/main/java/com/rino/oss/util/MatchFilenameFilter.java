package com.rino.oss.util;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * @author zip
 */
public class MatchFilenameFilter implements FilenameFilter {
    private String regex;

    public MatchFilenameFilter(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean accept(File dir, String name) {
        return StringUtils.isEmpty(regex) ? true : Pattern.matches(regex, name);
    }
}
