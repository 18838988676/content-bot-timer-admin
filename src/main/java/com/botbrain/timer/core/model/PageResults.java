package com.botbrain.timer.core.model;

import java.util.List;

/**
 * Copyright：botBrain.ai
 * Author: WangMingChao
 * Date: 2019/11/14.
 * Description:
 */
public class PageResults {

    private String recordsTotal;//总记录数
    private List<ResponseData> datas;

    public String getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(String recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public List<ResponseData> getDatas() {
        return datas;
    }

    public void setDatas(List<ResponseData> datas) {
        this.datas = datas;
    }
}
