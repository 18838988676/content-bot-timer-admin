package com.botbrain.timer.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Copyright：botBrain.ai
 * Author: WangMingChao
 * Date: 2019/11/14.
 * Description:  检测osk
 */
@Component
public class OskeyCheck {

    @Autowired
    ThreadPoolTaskExecutor checkExecutor;
//	@Autowired
//	private ConfigFeignClient configFeignClient;


    public OskeyCheck(){

    }

    //检测oskey的变化,并将所有带oskey的任务
    public void check(){
        //			List<Map<String, Object>> osList1 = configFeignClient.findAll(3, "").getData();
        List<Map<String, Object>> list=new ArrayList<>();



    }




}
