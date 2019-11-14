package com.botbrain.timer.core.model;

import java.util.List;

/**
 * Copyright：botBrain.ai
 * Author: WangMingChao
 * Date: 2019/11/14.
 * Description: 分组下的列表页
 */
public class ResponseData {
        private int id;				// 主键ID
        private Boolean isExtend=false; // 是否有子数据
        private String jobDesc;
        private String jobCron;		// 任务执行CRON表达式
        private String author;		// 负责人
        private int triggerStatus;		// 调度状态：0-停止，1-运行
        private String status;		// 调度状态：0-停止，1-运行
        private List<XxlJobInfo> xxlJobInfos;//该任务下的公司的任务执行信息

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Boolean getExtend() {
            return isExtend;
        }

        public void setExtend(Boolean extend) {
            isExtend = extend;
        }

        public String getJobDesc() {
            return jobDesc;
        }

        public void setJobDesc(String jobDesc) {
            this.jobDesc = jobDesc;
        }

        public String getJobCron() {
            return jobCron;
        }

        public void setJobCron(String jobCron) {
            this.jobCron = jobCron;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public int getTriggerStatus() {
            return triggerStatus;
        }

        public void setTriggerStatus(int triggerStatus) {
            setStatus(triggerStatus==0?"停止":"运行");
            this.triggerStatus = triggerStatus;
        }

        public List<XxlJobInfo> getXxlJobInfos() {
            return xxlJobInfos;
        }

        public void setXxlJobInfos(List<XxlJobInfo> xxlJobInfos) {
            this.xxlJobInfos = xxlJobInfos;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status =status;
        }
}
