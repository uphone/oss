package com.rino.oss.bean;

import lombok.Data;

/**
 * @author zip
 */
@Data
public class OSSFile {
    private String path;
    private String name;
    private Boolean dir;
    private Long size;
}
