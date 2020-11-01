package com.rino.oss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rino.oss.bean.ApiResult;
import com.rino.oss.bean.ErrorCode;
import com.rino.oss.bean.OSSFile;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zip
 */
@Slf4j
@Controller
public class FileUploadController {

    @Value("${rino.platform.rootPath}")
    private String rootPath;

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

    @ResponseBody
    @PostMapping("/children")
    public ApiResult listChildren(HttpServletRequest request) throws IOException {
        String path = request.getParameter("path");
        File file;
        if (StringUtils.isEmpty(path)) file = new File(rootPath);
        else file = new File(rootPath + path);
        String pattern = request.getParameter("pattern"); // 文件名称过滤
        File[] children;
        if (StringUtils.isEmpty(pattern)) {
            children = file.listFiles();
        } else {
            children = file.listFiles(new MatchFilenameFilter(pattern));
        }
        if (StringUtils.isEmpty(children)) return ApiResult.SUCCESS;
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
}
