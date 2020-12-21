package com.rino.oss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rino.oss.bean.ApiResult;
import com.rino.oss.bean.ErrorCode;
import com.rino.oss.bean.OSSFile;
import com.rino.oss.util.Compresser;
import com.rino.oss.util.MatchFilenameFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zip
 */
@Slf4j
@Controller
public class OSSController {

    @Value("${rino.platform.rootPath}")
    private String rootPath;

    /**
     * 上传文件
     * path: 上传的文件存储目录
     */
    @ResponseBody
    @PostMapping("/upload")
    public ApiResult uploadFile(HttpServletRequest request) throws IOException {
        // 判断path参数
        MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
        String path = multiRequest.getParameter("path");
        if (StringUtils.isEmpty(path)) return new ApiResult(ErrorCode.ERR_10001);
        Map<String, MultipartFile> fileMap = multiRequest.getFileMap();
        if (StringUtils.isEmpty(fileMap)) return new ApiResult(ErrorCode.ERR_10002);

        String dir = rootPath + path;
        File fileDir = new File(dir);
        if (!fileDir.exists()) fileDir.mkdirs();

        // 循环新增文件
        Set<String> keys = fileMap.keySet();
        for (String key : keys) {
            MultipartFile file = fileMap.get(key);
            String fileFullName = file.getOriginalFilename();
            File outFile = new File(fileDir + "/" + fileFullName);
            try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                StreamUtils.copy(file.getInputStream(), outputStream);
            }
            log.info("已上传文件:" + outFile.getPath());
        }
        return ApiResult.SUCCESS;
    }

    /**
     * 写入文件内容到指定文件
     * file: 写入的文件存储目录
     * content: 写入的文件内容
     * override: 是否覆盖已有文件(默认为false)
     */
    @ResponseBody
    @PostMapping("/write")
    public ApiResult writeFile(HttpServletRequest request) throws IOException {
        // 判断path参数
        String file = request.getParameter("file");
        if (StringUtils.isEmpty(file)) return new ApiResult(ErrorCode.ERR_10015);
        String content = request.getParameter("content");
        if (StringUtils.isEmpty(content)) return new ApiResult(ErrorCode.ERR_10016);
        String overrideStr = request.getParameter("override");
        boolean override = overrideStr != null && (overrideStr.equalsIgnoreCase("true") || overrideStr.equalsIgnoreCase("1"));
        File writeFile = new File(rootPath + file);
        if (writeFile.exists() && !override) return new ApiResult(ErrorCode.ERR_10017);
        FileUtils.writeStringToFile(writeFile, content);
        log.info("已写入文件:" + writeFile.getPath());
        return ApiResult.SUCCESS;
    }

    /**
     * 文件下载
     * file: 需要下载的文件
     */
    @PostMapping("/download")
    public void readFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filePath = request.getParameter("file");
        response.setCharacterEncoding("UTF-8");
        if (StringUtils.isEmpty(filePath)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ApiResult ret = new ApiResult(ErrorCode.ERR_10003);
            new ObjectMapper().writeValue(response.getOutputStream(), ret);
            return;
        }
        File file = new File(rootPath + filePath);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ApiResult ret = new ApiResult(ErrorCode.ERR_10004);
            new ObjectMapper().writeValue(response.getOutputStream(), ret);
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            StreamUtils.copy(inputStream, response.getOutputStream());
        }
        log.info("已下载文件:" + file.getPath());
    }

    /**
     * 获取目录中的所有文件及目录
     * path: 上级文件目录
     * regex: 文件过滤正则表达式
     */
    @ResponseBody
    @PostMapping("/children")
    public ApiResult listChildren(HttpServletRequest request) throws IOException {
        String path = request.getParameter("path");
        File file;
        if (StringUtils.isEmpty(path)) file = new File(rootPath);
        else file = new File(rootPath + path);
        String regex = request.getParameter("regex");
        File[] children;
        if (StringUtils.isEmpty(regex)) {
            children = file.listFiles();
        } else {
            children = file.listFiles(new MatchFilenameFilter(regex));
        }
        if (StringUtils.isEmpty(children)) return ApiResult.SUCCESS;

        // 排序处理
        String sort = request.getParameter("sort");
        if (sort != null && "asc".equalsIgnoreCase(sort)) Arrays.sort(children, Comparator.comparing(File::lastModified));
        else if (sort != null && "desc".equalsIgnoreCase(sort)) Arrays.sort(children, Comparator.comparing(File::lastModified).reversed());

        int len = children.length;
        List<OSSFile> ret = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            File child = children[i];
            boolean dir = child.isDirectory();
            String childPath = child.getPath().substring(rootPath.length());
            OSSFile ossFile = new OSSFile();
            ossFile.setPath(childPath);
            ossFile.setDir(dir);
            ossFile.setName(child.getName());
            if (dir) {
                ossFile.setSize(FileUtils.sizeOfDirectory(child));
            } else {
                ossFile.setSize(child.length());
            }
            ret.add(ossFile);
        }
        return new ApiResult(ret);
    }

    /**
     * 删除文件
     * path: 需要删除的文件
     */
    @ResponseBody
    @PostMapping("/delete")
    public ApiResult deleteFile(HttpServletRequest request) {
        String path = request.getParameter("path");
        if (StringUtils.isEmpty(path)) return new ApiResult(ErrorCode.ERR_10005);
        File file = new File(rootPath + path);
        if (!file.exists()) return new ApiResult(ErrorCode.ERR_10004);
        if (file.isDirectory()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                return new ApiResult(-1, e.getMessage());
            }
        } else {
            file.delete();
        }
        log.info("已删除文件或目录:" + file.getPath());
        return ApiResult.SUCCESS;
    }

    /**
     * 询问是否存在某文件
     * path: 文件路径
     */
    @ResponseBody
    @PostMapping("/ask")
    public ApiResult askFile(HttpServletRequest request) {
        String path = request.getParameter("path");
        if (StringUtils.isEmpty(path)) return new ApiResult(ErrorCode.ERR_10006);
        File file = new File(rootPath + path);
        return new ApiResult(file.exists());
    }

    /**
     * 创建目录
     * path: 目录路径
     */
    @ResponseBody
    @PostMapping("/mkdir")
    public ApiResult mkDir(HttpServletRequest request) throws IOException {
        String path = request.getParameter("path");
        if (StringUtils.isEmpty(path)) return new ApiResult(ErrorCode.ERR_10013);
        File file = new File(rootPath + path);
        if (file.exists()) return new ApiResult(ErrorCode.ERR_10014);
        FileUtils.forceMkdir(file);
        return ApiResult.SUCCESS;
    }

    /**
     * 复制文件或目录
     * src: 需要复制的文件或目录
     * tar: 复制目标
     */
    @ResponseBody
    @PostMapping("/copy")
    public ApiResult copy(HttpServletRequest request) throws IOException {
        String src = request.getParameter("src");
        String tar = request.getParameter("tar");
        if (StringUtils.isEmpty(src) || StringUtils.isEmpty(tar)) return new ApiResult(ErrorCode.ERR_10007);
        File srcFile = new File(rootPath + src);
        if (!srcFile.exists()) return new ApiResult(ErrorCode.ERR_10004);
        File tarFile = new File(rootPath + tar);
        if (srcFile.isDirectory()) FileUtils.copyDirectory(srcFile, tarFile);
        else FileUtils.copyFile(srcFile, tarFile);
        log.info("已复制文件或目录:[" + srcFile.getPath() + "] -> [" + tarFile.getPath() + "]");
        return ApiResult.SUCCESS;
    }

    /**
     * 压缩目录
     * path: 需要压缩的目录
     * regex: 文件过滤正则表达式
     * fileName: 存储目标文件
     * cutPart: 去除路径部分
     */
    @ResponseBody
    @PostMapping("/zip/dir")
    public ApiResult zipDir(HttpServletRequest request) throws IOException {
        String path = request.getParameter("path");
        if (StringUtils.isEmpty(path)) return new ApiResult(ErrorCode.ERR_10008);
        String regex = request.getParameter("regex");
        String fileName = request.getParameter("fileName");
        String cutPart = request.getParameter("cutPart");
        Compresser compresser = new Compresser();
        compresser.setRootPath(rootPath);
        compresser.setDir(path);
        compresser.setRegex(regex);
        compresser.setFileName(fileName);
        compresser.setCutPart(cutPart);
        OSSFile ossFile = compresser.compressDir();
        log.info("已压缩目录:[" + path + "] -> [" + ossFile.getPath() + "]");
        return new ApiResult(ossFile);
    }

    /**
     * 压缩多个文件:
     * src: 压缩的源文件
     * tar: 压缩文件存储目标位置
     * regex: 目录中的文件过滤表达式
     * cutPart: 去除路径部分
     */
    @ResponseBody
    @PostMapping("/zip/files")
    public ApiResult zipFiles(HttpServletRequest request) throws IOException {
        String[] files = request.getParameterValues("src");
        if (files == null || files.length == 0) return new ApiResult(ErrorCode.ERR_10011);
        String tar = request.getParameter("tar");
        if (StringUtils.isEmpty(tar)) return new ApiResult(ErrorCode.ERR_10012);
        String regex = request.getParameter("regex");
        String cutPart = request.getParameter("cutPart");
        Compresser compresser = new Compresser();
        compresser.setRootPath(rootPath);
        compresser.setFileName(tar);
        compresser.setFiles(files);
        compresser.setRegex(regex);
        compresser.setCutPart(cutPart);
        OSSFile ossFile = compresser.compressFiles();
        log.info("已压缩[" + files.length + "]个文件 -> [" + ossFile.getPath() + "]");
        return new ApiResult(ossFile);
    }

    /**
     * 压缩并下载目录
     * path: 需要下载的目录
     * regex: 文件名称过滤正则表达式
     * cutPart: 去除路径部分
     */
    @PostMapping("/download/dir")
    public void downloadDirZip(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getParameter("path");
        if (StringUtils.isEmpty(path)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ApiResult ret = new ApiResult(ErrorCode.ERR_10009);
            new ObjectMapper().writeValue(response.getOutputStream(), ret);
            return;
        }
        String regex = request.getParameter("regex");
        String cutPart = request.getParameter("cutPart");
        Compresser compresser = new Compresser();
        compresser.setRootPath(rootPath);
        compresser.setDir(path);
        compresser.setRegex(regex);
        compresser.setCutPart(cutPart);
        compresser.downloadDir(response.getOutputStream());
        log.info("已压缩并下载目录:[" + path + "]");
    }

    /**
     * 压缩并下载多个文件或目录
     * src: 需要压缩的文件或目录
     * regex: 文件或目录名称过滤正则表达式
     * cutPart: 去除路径部分
     */
    @PostMapping("/download/files")
    public void downloadFilesZip(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] files = request.getParameterValues("src");
        if (files == null || files.length == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ApiResult ret = new ApiResult(ErrorCode.ERR_10010);
            new ObjectMapper().writeValue(response.getOutputStream(), ret);
            return;
        }
        String regex = request.getParameter("regex");
        String cutPart = request.getParameter("cutPart");
        Compresser compresser = new Compresser();
        compresser.setRootPath(rootPath);
        compresser.setFiles(files);
        compresser.setRegex(regex);
        compresser.setCutPart(cutPart);
        compresser.downloadFiles(response.getOutputStream());
        log.info("已压缩[" + files.length + "]个文件或目录并下载");
    }
}
