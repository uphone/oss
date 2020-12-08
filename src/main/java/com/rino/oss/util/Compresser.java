package com.rino.oss.util;

import com.rino.oss.bean.OSSFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author zip
 */
public class Compresser {
    private Set<String> zipDirKeys = new HashSet<>(16);

    private String rootPath;
    private String dir;
    private String regex;
    private String fileName;
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


    private OSSFile getZippedOssFile(String compressFileName) {
        File zippedFile = new File(compressFileName.substring(rootPath.length()));
        OSSFile ossFile = new OSSFile();
        ossFile.setPath(compressFileName);
        ossFile.setDir(false);
        ossFile.setName(zippedFile.getName());
        ossFile.setSize(zippedFile.length());
        return ossFile;
    }

    public OSSFile compressDir() throws IOException {
        File file = new File(rootPath + dir);
        String compressFileName;
        if (StringUtils.isEmpty(this.fileName)) compressFileName = file.getParentFile().getPath() + "/" + file.getName() + ".zip";
        else compressFileName = rootPath + fileName;
        try (
                FileOutputStream fos = new FileOutputStream(compressFileName);
                ZipOutputStream cos = new ZipOutputStream(fos);
        ) {
            compressFile(cos, file);
        }
        return getZippedOssFile(compressFileName);
    }

    public OSSFile compressFiles() throws IOException {
        String compressFileName = rootPath + this.fileName;
        try (
                FileOutputStream fos = new FileOutputStream(compressFileName);
                ZipOutputStream cos = new ZipOutputStream(fos);
        ) {
            for (String file : files) compressFile(cos, new File(rootPath + file));
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
                cos.putNextEntry(new ZipEntry(dir + "/"));
                cos.closeEntry();
            }
            cos.putNextEntry(new ZipEntry(filePath));
            try (FileInputStream in = new FileInputStream(file);) {
                int len;
                while ((len = in.read()) != -1) cos.write(len);
            }
        }
    }

    public static void main(String[] args) {
        Compresser c = new Compresser();
        c.setRootPath("/Users/albert/Downloads/AServer/nginx-apt-biocloud-6094");
        c.setFiles(new String[]{
                "/tool/task/777-4",
                "/report/html/TMT/TMT-1.html",
                "/index.html",
                "/report/4/TMT/P20200601109/Result/Schema/差异结果数量统计"
        });
        c.setRegex("[^_].*");
        c.setFileName("/aa.zip");
        try {
            c.compressFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
