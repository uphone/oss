package com.rino.oss.bean;

/**
 * @author zip
 */
public enum ErrorCode {
    ERR_10001(-10001, "参数不能为空：上传路径[path]"),
    ERR_10002(-10002, "上传文件不能为空"),
    ERR_10003(-10003, "参数不能为空：下载文件路径[file]"),
    ERR_10004(-10004, "文件不存在"),
    ERR_10005(-10005, "参数不能为空：删除文件路径[path]"),

    ERR_500(-500, "系统内部错误");


    private int status;
    private String message;

    private ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() {
        return this.status;
    }

    public String message() {
        return this.message;
    }
}