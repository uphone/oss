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
    ERR_10006(-10006, "参数不能为空：询问文件路径[path]"),
    ERR_10007(-10007, "参数不能为空：复制源文件及目标文件[src,tar]"),
    ERR_10008(-10008, "参数不能为空：压缩目录[path]"),
    ERR_10009(-10009, "参数不能为空：下载目录[path]"),
    ERR_10010(-10010, "参数不能为空：下载文件[src]"),
    ERR_10011(-10011, "参数不能为空：待压缩文件或目录[src]"),
    ERR_10012(-10012, "参数不能为空：待存储的压缩文件[tar]"),
    ERR_10013(-10013, "参数不能为空：待创建的文件目录[path]"),
    ERR_10014(-10014, "文件目录已存在"),
    ERR_10015(-10015, "参数不能为空：写入文件路径[file]"),
    ERR_10016(-10016, "参数不能为空：写入文件内容[content]"),
    ERR_10017(-10017, "写入文件已存在"),
    ERR_10018(-10018, "参数不能为空：待更新的文件或目录[path]"),
    ERR_10019(-10019, "待更新的文件或目录不存在"),
    ERR_10020(-10020, "文件或目录的更新时间格式不正确"),

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