package com.botbrain.timer.service.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Copyright：botBrain.ai
 * @Author: WangMingChao
 * @Date: 2019/11/19.
 * @Description:
 */
public class ParamEntity {
    private String url;//url
    private String osType;
    private List<String> filterOs;//过滤
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public List<String> getFilterOs() {
        return filterOs;
    }

    public void setFilterOs(List<String> filterOs) {
        this.filterOs = filterOs;
    }

}
