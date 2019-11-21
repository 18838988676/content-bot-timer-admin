package com.botbrain.timer.schedule;

import com.botbrain.sdk.inner.client.config.ConfigFeignClient;
import com.botbrain.timer.core.model.XxlJobInfo;
import com.botbrain.timer.dao.XxlJobInfoDao;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Copyright：botBrain.ai
 * Author: WangMingChao
 * Date: 2019/11/14.
 * Description:  检测osk
 */
@Configuration
public class OskeyCheck {

    private volatile List<String> osKeyLocal = new CopyOnWriteArrayList<String>();
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Autowired
    private ScheduledExecutorService scheduledExecutorServiceCheck;

    @Autowired
    private ConfigFeignClient configFeignClient;

    public OskeyCheck() {

    }

    @PostConstruct
    private void startCheck() {
        //初始化本地oskey
        initLocalOsk();
        //5分钟后启动定时任务，每12小时执行一次；
        scheduledExecutorServiceCheck.scheduleWithFixedDelay(() -> {
            System.out.println("os_key 检测线程："+Thread.currentThread().getName());
            checkCenter();
        }, 2, 5, TimeUnit.SECONDS);
    }


    //检测oskey的变化,并将所有带oskey的任务
    private void checkCenter() {
        //获得osKey
        ArrayList<String> list = new ArrayList<>();
        list.add("e");
        list.add("123");

        //判断，osKey是增加 还是减少
        Map<String, List<String>> results = check(list);
        if (results.containsKey("add")) {
            //执行新增任务
            System.out.println("执行新增任务");
//            addJod(results.get("add"));
        }
        if (results.containsKey("remove")) {
            //执行删除任务
            System.out.println("执行删除任务");
//            removeJod(results.get("remove"));
        }
    }

    //第一次启动将库中osk加到local中
    private void initLocalOsk() {
        //获得osKey
        List<String> osks = new ArrayList<>(20);
        osks.add("a");
        osks.add("b");
        osks.add("c");
        osks.add("d");
        osks.add("e");
        if (!CollectionUtils.isEmpty(osks)) {
            osKeyLocal.addAll(osks);
        }
    }

    private void addJod(List<String> oskeys) {

        List<XxlJobInfo> dbxxlJobInfoConfigList = xxlJobInfoDao.findJobInfoConfigPublic();

        List<XxlJobInfo> xxlJobInfoList = new ArrayList<>(30);
        for (XxlJobInfo jobInfo : xxlJobInfoList) {
            //查询公共任务的job配置
            for (String dataos : oskeys) {
                XxlJobInfo newXxlJobInfo = new XxlJobInfo();
                BeanUtils.copyProperties(jobInfo, newXxlJobInfo);
                newXxlJobInfo.setJobInfoGroupParentId(jobInfo.getId());
                newXxlJobInfo.setExecutorParam(jobInfo.getExecutorParam().replace("{os_key}", dataos));
                newXxlJobInfo.setJobDesc(jobInfo.getJobDesc() + "任务下发到:" + dataos);
                xxlJobInfoList.add(newXxlJobInfo);
            }
        }
        int num = xxlJobInfoDao.saveBatch(xxlJobInfoList);
    }

    private void removeJod(List<String> oskeys) {
        List<Integer> ids = xxlJobInfoDao.findJobInfoConfigPublicId();
        for (String str : oskeys) {
//            xxlJobInfoDao.deleteBatch(ids, str);
        }
    }

    //判断
    private Map<String, List<String>> check(ArrayList<String> newOskeylists) {
        Map<String, List<String>> results = new HashMap<>();

        //留个副本
        List newOskeyListCopy = new ArrayList<>();
        newOskeyListCopy.addAll(newOskeylists);
        List osKeyLocalCopy = new CopyOnWriteArrayList<>();
        osKeyLocalCopy.addAll(osKeyLocal);

        //差集
        newOskeyListCopy.removeAll(osKeyLocalCopy);
        if (newOskeyListCopy.size() != 0) {
            //新增的
            results.put("add", newOskeyListCopy);
            osKeyLocal.addAll(newOskeyListCopy);
        }
        //差集
        osKeyLocalCopy.removeAll(newOskeylists);
        if (osKeyLocalCopy.size() != 0) {
            //减少的
            results.put("remove", osKeyLocalCopy);
            osKeyLocal.removeAll(osKeyLocalCopy);
        }
        return results;
    }
}
