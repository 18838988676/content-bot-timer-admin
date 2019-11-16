package com.botbrain.timer.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Copyright：botBrain.ai
 * @author: WangMingChao
 * @Date: 2019/11/5.
 * @Description:数据去重== 重复报警
 **/
@Component
public class IntervalAlarmServiceImpl implements IntervalAlarmService {

    private ConcurrentHashMap<String, Long> concurrentHashMap = new ConcurrentHashMap<String, Long>();

    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            int num = 0;
            return new Thread(r, "saveAlarmForInterval-thread:" + num++);
        }
    });

    public IntervalAlarmServiceImpl() {
        scheduledExecutorService.scheduleWithFixedDelay(this::removeOutOfData,1,2,TimeUnit.SECONDS);
    }


    /**
     * 剔除已经过期的key
     */
    private void removeOutOfData() {
        System.out.println(Thread.currentThread().getName());
        long nowTime= System.currentTimeMillis();
        if (!CollectionUtils.isEmpty(concurrentHashMap)) {
            for (Map.Entry<String, Long> entry : concurrentHashMap.entrySet()) {
                //如果map中的时间在当前时间之前，表示已过期，删除key
                if (isBefore(entry.getValue(),nowTime)) {
                    concurrentHashMap.remove(entry.getKey());
                }
            }
        }
    }

    /**
     * 判断数据是否存在
     *
     * @param id     模板id
     * @param toUser 接收者
     * @return true:存在
     */
    @Override
    public Boolean isDataHaveResult(String id, String toUser) {
        return concurrentHashMap.containsKey(toUser + id);
    }
    /**
     * 判断数据是否存在
     *
     * @param data     数据
     * @param time 数据保存时间
     * @return true:存在
     */
    @Override
    public Boolean isHaveOrSave(String data, Integer time){
        Boolean ishave=Boolean.FALSE;
        if(!concurrentHashMap.containsKey(data)){
            concurrentHashMap.put(data, getDate2(time));
            ishave=Boolean.TRUE;
        }
        return ishave;
    }

    /**
     * 保存数据，如果数据不存在则保存数据并返回true，否则返回false
     *
     * @param id     模板id
     * @param toUser 接收者
     * @param time   间隔时间
     * @return true:不重复
     */
    @Override
    public Boolean saveAndGetResult(String id, String toUser, int time) {
        if (!isDataHaveResult(id, toUser)) {
            concurrentHashMap.put(toUser + id, getDate(time));
            return true;
        }
        return false;
    }

    /**
     * 保存数据，如果数据不存在则保存数据并返回true，否则返回false
     *
     * @param id     模板id
     * @param toUser 接收者

     * @return true:不重复
     */
    @Override
    public Boolean saveAndGetResult(String id, String toUser) {
        if (!isDataHaveResult(id, toUser)) {
            concurrentHashMap.put(toUser + id, getDate2(40));
            return true;
        }
        return false;
    }

    /**
     * 获得指定分钟数后的时间
     *
     * @param amount
     * @return
     */
    private Long getDate(int amount) {
        return System.currentTimeMillis() + amount * 1000 * 60L;
    }

    /**
     * 获得指定分钟数后的时间
     *
     * @param amount
     * @return
     */
    private Long getDate2(int amount) {
        return System.currentTimeMillis() + amount * 1000 ;
    }

    /**
     * 判断时间先后
     *
     * @param time
     * @return
     */
    private Boolean isBefore(long time,long nowTime) {
        return time < nowTime;
    }

}
