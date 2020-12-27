package com.rino.oss.util;

import com.rino.oss.bean.OSSFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author zip
 * 命令行压缩: tar -zcvf bak.tar.gz --exclude='_*' 差异结果数量统计
 */
public class Compresser {
    private Set<String> zipDirKeys = new HashSet<>(16);

    private String rootPath;
    private String dir;
    private String regex;
    private String fileName;
    private String cutPart;
    private String[] files;

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setCutPart(String cutPart) {
        this.cutPart = cutPart;
    }

    private OSSFile getZippedOssFile(String compressFileName) {
        File zippedFile = new File(compressFileName.substring(rootPath.length()));
        OSSFile ossFile = new OSSFile();
        ossFile.setPath(compressFileName);
        ossFile.setDir(false);
        ossFile.setName(zippedFile.getName());
        ossFile.setSize(zippedFile.length());
        return ossFile;
    }

    public void downloadDir(OutputStream outputStream) throws IOException {
        File file = new File(rootPath + dir);
        try (ZipOutputStream cos = new ZipOutputStream(outputStream)) {
            compressFile(cos, file);
        }
    }

    public OSSFile compressDir() throws IOException {
        File file = new File(rootPath + dir);
        String compressFileName;
        if (StringUtils.isEmpty(this.fileName)) compressFileName = file.getParentFile().getPath() + "/" + file.getName() + ".zip";
        else compressFileName = rootPath + fileName;
        try (FileOutputStream fos = new FileOutputStream(compressFileName)) {
            downloadDir(fos);
        }
        return getZippedOssFile(compressFileName);
    }

    public void downloadFiles(OutputStream outputStream) throws IOException {
        try (ZipOutputStream cos = new ZipOutputStream(outputStream)) {
            for (String file : files) compressFile(cos, new File(rootPath + file));
        }
    }

    public OSSFile compressFiles() throws IOException {
        String compressFileName = rootPath + this.fileName;
        try (FileOutputStream fos = new FileOutputStream(compressFileName)) {
            downloadFiles(fos);
        }
        return getZippedOssFile(compressFileName);
    }

    private void compressFile(ZipOutputStream cos, File file) throws IOException {
        if (file.isDirectory()) {
            File[] children;
            if (StringUtils.isEmpty(regex)) {
                children = file.listFiles();
            } else {
                children = file.listFiles(new MatchFilenameFilter(regex));
            }
            for (int i = 0; i < children.length; i++) compressFile(cos, children[i]);
        } else {
            String filePath = file.getPath().substring(rootPath.length());
            String dir = filePath.substring(0, filePath.lastIndexOf("/")).trim();
            if (!StringUtils.isEmpty(dir) && !zipDirKeys.contains(dir)) {
                zipDirKeys.add(dir);
                String dirEntry = dir + "/";
                if (!StringUtils.isEmpty(this.cutPart)) dirEntry = dirEntry.substring(this.cutPart.length());
                cos.putNextEntry(new ZipEntry(dirEntry));
                cos.closeEntry();
            }
            String fileEntry = filePath;
            if (!StringUtils.isEmpty(this.cutPart)) fileEntry = fileEntry.substring(this.cutPart.length());
            cos.putNextEntry(new ZipEntry(fileEntry));
            try (FileInputStream in = new FileInputStream(file);) {
                int len;
                while ((len = in.read()) != -1) cos.write(len);
            }
        }
    }

    public static void main(String[] args) {
    }
}
