package com.rino.oss.bean;

/**
 * @author zip
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiResult {

    public static final ApiResult SUCCESS = new ApiResult(1, null);

    public ApiResult() {
    }

    public ApiResult(Integer status, String message) {
        this.status = status;
        this.message = message;
    }

    public ApiResult(Object data) {
        this.status = 1;
        this.data = data;
    }

    public ApiResult(ErrorCode errorCode) {
        this.status = errorCode.status();
        this.message = errorCode.message();
    }

    protected Integer status;
    protected String message;
    protected Object data;
}
