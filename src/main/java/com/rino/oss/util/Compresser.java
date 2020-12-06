package com.rino.oss.util;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author zip
 */
public class Compresser {
    private String dir;
    private String regex;
    private String fileName;

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public File compress() throws IOException {
        File file = new File(dir);
        if (StringUtils.isEmpty(this.fileName)) this.fileName = file.getName() + ".zip";
        String compressFileName = file.getParentFile().getPath() + "/" + fileName;
        try (
                FileOutputStream fos = new FileOutputStream(compressFileName);
                ZipOutputStream cos = new ZipOutputStream(fos);
        ) {
            compressFile(cos, file, "");
        }
        return new File(compressFileName);
    }

    private void compressFile(ZipOutputStream cos, File file, String base) throws IOException {
        if (file.isDirectory()) {
            File[] children;
            if (StringUtils.isEmpty(regex)) {
                children = file.listFiles();
            } else {
                children = file.listFiles(new MatchFilenameFilter(regex));
            }
            cos.putNextEntry(new ZipEntry(base + "/"));
            base = (base.length() == 0 ? "" : base + "/");
            for (int i = 0; i < children.length; i++) {
                compressFile(cos, children[i], base + children[i].getName());
            }
        } else {
            if (base == "") base = file.getName();
            cos.putNextEntry(new ZipEntry(base));
            try (FileInputStream in = new FileInputStream(file);) {
                int len;
                while ((len = in.read()) != -1) cos.write(len);
            }
        }

    }
}
