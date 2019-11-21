package com.botbrain.timer.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.botbrain.sdk.inner.client.config.ConfigFeignClient;
import com.botbrain.timer.core.cron.CronExpression;
import com.botbrain.timer.core.model.PageResults;
import com.botbrain.timer.core.model.ResponseData;
import com.botbrain.timer.core.model.XxlJobGroup;
import com.botbrain.timer.core.model.XxlJobInfo;
import com.botbrain.timer.core.route.ExecutorRouteStrategyEnum;
import com.botbrain.timer.core.thread.JobScheduleHelper;
import com.botbrain.timer.core.util.I18nUtil;
import com.botbrain.timer.dao.XxlJobGroupDao;
import com.botbrain.timer.dao.XxlJobInfoDao;
import com.botbrain.timer.dao.XxlJobLogDao;
import com.botbrain.timer.dao.XxlJobLogGlueDao;
import com.botbrain.timer.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;

/**
 * core job action for xxl-job
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
@Service
public class XxlJobServiceImpl implements XxlJobService {
    private static Logger logger = LoggerFactory.getLogger(XxlJobServiceImpl.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    public XxlJobLogDao xxlJobLogDao;
    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;
    @Autowired
    private ConfigFeignClient configFeignClient;

    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

        // page list
        List<XxlJobInfo> list = xxlJobInfoDao.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        int list_count = xxlJobInfoDao.pageListCount(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);        // 总记录数
        maps.put("recordsFiltered", list_count);    // 过滤后的总记录数
        maps.put("data", list);                    // 分页列表
        return maps;
    }

    @Override
    public PageResults pageListByGroup(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        PageResults pageResults = new PageResults();

        //查询出所有顶级任务（jobInfoGroupParentId=-1），但不包含具体公司
        List<XxlJobInfo> xxlJobInfos = xxlJobInfoDao.pageListByGroup(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        if (CollectionUtils.isEmpty(xxlJobInfos)) {
            return pageResults;
        }
        int list_count = xxlJobInfoDao.pageListCountByGroup(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);

        List<ResponseData> list = new ArrayList<>(20);
        xxlJobInfos.stream().forEach(xxlJobInfo -> {
            //顶级任务列表
            ResponseData responseData = new ResponseData();
            BeanUtils.copyProperties(xxlJobInfo, responseData);
            //具体公司任务列表
            List<XxlJobInfo> jobs = xxlJobInfoDao.getAllJobsByGroupId(xxlJobInfo.getId());
            if (!CollectionUtils.isEmpty(jobs)) {
                responseData.setExtend(true);
                responseData.setXxlJobInfos(jobs);
            }
            list.add(responseData);
        });
        pageResults.setRecordsTotal(String.valueOf(list_count));
        pageResults.setDatas(list);
        return pageResults;
    }

    @Override
    public ReturnT<String> add(XxlJobInfo jobInfo) {
        // valid
        XxlJobGroup group = xxlJobGroupDao.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid"));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && (jobInfo.getExecutorHandler() == null || jobInfo.getExecutorHandler().trim().length() == 0)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
        }

        // fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource() != null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // ChildJobId valid
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<String>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<String>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);

            jobInfo.setChildJobId(temp);
        }

        // add in db  针对os_key问题。
        // -1代表任务，非-1代表某一任务下的子任务；
        //{"任务ID":"1","任务名称":"点名","cron":"******","JobInfoGroupParentId":"-1",......},
        //{"任务ID":"2","任务名称":"百度公司执行点名任务","cron":"******","JobInfoGroupParentId":"1",......},
        //{"任务ID":"3","任务名称":"新浪公司执行点名任务","cron":"******","JobInfoGroupParentId":"1",......},
        // 不存在os_key问题时的任务添加：也会添加-1，只是列表页是否扩展问题上，会判断下该id下是否有子数据
        ParamEntity paramEntity = new ParamEntity();
        Boolean isTrue = Boolean.FALSE;
        if (StringUtils.isEmpty(jobInfo.getExecutorParam())) {
            //任务中不带url参数的；简单任务
            jobInfo.setJobInfoGroupParentId(0);
        } else {
            paramEntity = JSONObject.parseObject(jobInfo.getExecutorParam(), ParamEntity.class);
            if(!StringUtils.isEmpty(paramEntity.getUrl())&&paramEntity.getUrl().contains("{os_key}"))
            {
                //任务中带url，且url中带os_key；复杂任务
                jobInfo.setJobInfoGroupParentId(-1);
                //仅表示JobInfoGroupParentId已被设为-1，同时表示此时是
                isTrue = Boolean.TRUE;
            }else{
                //任务中带url，但url中不带os_key；简单
                jobInfo.setJobInfoGroupParentId(0);
            }
        }
        xxlJobInfoDao.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
        }

        Integer resultNum;
        if (jobInfo.getId() > 0 && isTrue) {
            resultNum=addOtherJob(jobInfo, paramEntity);
        }

        return new ReturnT<String>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str) {
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Integer addOtherJob(XxlJobInfo jobInfo, ParamEntity paramEntity) {
        //先 判断一下 如果任务插入成功后并且任务中的执行参数url中带有os_key的话  就将各企业按照此任务配置进行批量配置
        // 0是全查；
        Integer osType = StringUtils.isEmpty(paramEntity.getOsType()) ? 0 :Integer.parseInt(paramEntity.getOsType()) ;
        List<Map<String, Object>> os = configFeignClient.findAll(osType, null).getData();
        List<String> osList = new ArrayList<>();
        for (Map<String, Object> o : os) {
            //获得os列表
            osList.add((String) o.get("os_key"));
        }
//			List<String> osList=new ArrayList<>();
//			osList.add("2WIQRCZAPA");
//			osList.add("4TNNX4YCFF");
//			osList.add("9JSPXUZPVD");
//			osList.add("AF3NSIWP4X");
        if (!CollectionUtils.isEmpty(paramEntity.getFilterOs())) {
            //过滤一些oskey
            osList.removeAll(paramEntity.getFilterOs());
        }

        List<XxlJobInfo> xxlJobInfoList = new ArrayList<>(30);
        for (String dataos : osList) {
            XxlJobInfo newXxlJobInfo = new XxlJobInfo();
            BeanUtils.copyProperties(jobInfo, newXxlJobInfo);
            newXxlJobInfo.setJobInfoGroupParentId(jobInfo.getId());
            newXxlJobInfo.setExecutorParam(paramEntity.getUrl().replace("{os_key}", dataos));
            newXxlJobInfo.setJobDesc(jobInfo.getJobDesc() + "任务下发到:" + dataos);
            xxlJobInfoList.add(newXxlJobInfo);
        }
        return xxlJobInfoDao.saveBatch(xxlJobInfoList);
    }


    @Override
    public ReturnT<String> update(XxlJobInfo jobInfo) {

        // valid
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid"));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }

        // ChildJobId valid
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<String>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<String>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length() - 1);

            jobInfo.setChildJobId(temp);
        }

        // group valid
        XxlJobGroup jobGroup = xxlJobGroupDao.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid")));
        }

        // stage job info
        XxlJobInfo exists_jobInfo = xxlJobInfoDao.loadById(jobInfo.getId());
        if (exists_jobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = exists_jobInfo.getTriggerNextTime();
        if (exists_jobInfo.getTriggerStatus() == 1 && !jobInfo.getJobCron().equals(exists_jobInfo.getJobCron())) {
            try {
                Date nextValidTime = new CronExpression(jobInfo.getJobCron()).getNextValidTimeAfter(new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_never_fire"));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid") + " | " + e.getMessage());
            }
        }

        exists_jobInfo.setJobGroup(jobInfo.getJobGroup());
        exists_jobInfo.setJobCron(jobInfo.getJobCron());
        exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
        exists_jobInfo.setAuthor(jobInfo.getAuthor());
        exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
        exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        exists_jobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        exists_jobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        exists_jobInfo.setChildJobId(jobInfo.getChildJobId());
        exists_jobInfo.setTriggerNextTime(nextTriggerTime);
        xxlJobInfoDao.update(exists_jobInfo);


        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> remove(int id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
        if (xxlJobInfo == null) {
            return ReturnT.SUCCESS;
        }

        xxlJobInfoDao.delete(id);
        xxlJobLogDao.delete(id);
        xxlJobLogGlueDao.deleteByJobId(id);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> start(int id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = 0;
        try {
            Date nextValidTime = new CronExpression(xxlJobInfo.getJobCron()).getNextValidTimeAfter(new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_never_fire"));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid") + " | " + e.getMessage());
        }

        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);

        xxlJobInfoDao.update(xxlJobInfo);

        //  启动这个任务后，判断下面有没有子任务，有的话 全部执行；
        if(-1==xxlJobInfo.getJobInfoGroupParentId()){
            // 获得该任务下的子任务的id;
             List<Integer> childrenIds = xxlJobInfoDao.findChildrenJobInfoIdByParentId(xxlJobInfo.getId());
            startBatch(xxlJobInfo.getId(),childrenIds);
        }
        return ReturnT.SUCCESS;
    }


    @Override
    public ReturnT<String> stop(int id) {
        XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);

        xxlJobInfo.setTriggerStatus(0);
        xxlJobInfo.setTriggerLastTime(0);
        xxlJobInfo.setTriggerNextTime(0);

        xxlJobInfoDao.update(xxlJobInfo);

        //  停止这个任务后，判断下面有没有子任务，有的话 全部停止；
        if(-1==xxlJobInfo.getJobInfoGroupParentId()){
            // 获得该任务下的子任务的id;
            List<Integer> childrenIds = xxlJobInfoDao.findChildrenJobInfoIdByParentId(xxlJobInfo.getId());
            pauseBatch(xxlJobInfo.getId(),childrenIds);
        }


        return ReturnT.SUCCESS;
    }

    @Override
    public Map<String, Object> dashboardInfo() {

        int jobInfoCount = xxlJobInfoDao.findAllCount();
        int jobLogCount = xxlJobLogDao.triggerCountByHandleCode(-1);
        int jobLogSuccessCount = xxlJobLogDao.triggerCountByHandleCode(ReturnT.SUCCESS_CODE);

        // executor count
        Set<String> executerAddressSet = new HashSet<String>();
        List<XxlJobGroup> groupList = xxlJobGroupDao.findAll();

        if (groupList != null && !groupList.isEmpty()) {
            for (XxlJobGroup group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executerAddressSet.addAll(group.getRegistryList());
                }
            }
        }

        int executorCount = executerAddressSet.size();

        Map<String, Object> dashboardMap = new HashMap<String, Object>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    private static final String TRIGGER_CHART_DATA_CACHE = "trigger_chart_data_cache";

    @Override
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
		/*// get cache
		String cacheKey = TRIGGER_CHART_DATA_CACHE + "_" + startDate.getTime() + "_" + endDate.getTime();
		Map<String, Object> chartInfo = (Map<String, Object>) LocalCacheUtil.get(cacheKey);
		if (chartInfo != null) {
			return new ReturnT<Map<String, Object>>(chartInfo);
		}*/

        // process
        List<String> triggerDayList = new ArrayList<String>();
        List<Integer> triggerDayCountRunningList = new ArrayList<Integer>();
        List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
        List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<Map<String, Object>> triggerCountMapAll = xxlJobLogDao.triggerCountByDay(startDate, endDate);
        if (triggerCountMapAll != null && triggerCountMapAll.size() > 0) {
            for (Map<String, Object> item : triggerCountMapAll) {
                String day = String.valueOf(item.get("triggerDay"));
                int triggerDayCount = Integer.valueOf(String.valueOf(item.get("triggerDayCount")));
                int triggerDayCountRunning = Integer.valueOf(String.valueOf(item.get("triggerDayCountRunning")));
                int triggerDayCountSuc = Integer.valueOf(String.valueOf(item.get("triggerDayCountSuc")));
                int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);

                triggerCountRunningTotal += triggerDayCountRunning;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = 4; i > -1; i--) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), -i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);

        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);

		/*// set cache
		LocalCacheUtil.set(cacheKey, result, 60*1000);     // cache 60s*/

        return new ReturnT<Map<String, Object>>(result);
    }

    @Override
    public ReturnT<String> startBatch(Integer groupId, List<Integer> childrenIds ) {

            for (Integer id : childrenIds) {
                XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
                if (xxlJobInfo != null) {
                    // next trigger time (5s后生效，避开预读周期)
                    long nextTriggerTime = 0;
                    try {
                        Date nextValidTime = new CronExpression(xxlJobInfo.getJobCron()).getNextValidTimeAfter(new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                        if (nextValidTime == null) {
                            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_never_fire"));
                        }
                        nextTriggerTime = nextValidTime.getTime();
                    } catch (ParseException e) {
                        logger.error(e.getMessage(), e);
                        return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid") + " | " + e.getMessage());
                    }
                    xxlJobInfo.setTriggerStatus(1);
                    xxlJobInfo.setTriggerLastTime(0);
                    xxlJobInfo.setTriggerNextTime(nextTriggerTime);
                    xxlJobInfoDao.update(xxlJobInfo);

                }
            }
        return ReturnT.SUCCESS;
    }


    @Override
    public ReturnT<String> pauseBatch(Integer groupId, List<Integer> childrenIds) {
            for (Integer id : childrenIds) {
                XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
                if (xxlJobInfo != null) {
                    xxlJobInfo.setTriggerStatus(0);
                    xxlJobInfo.setTriggerLastTime(0);
                    xxlJobInfo.setTriggerNextTime(0);
                    xxlJobInfoDao.update(xxlJobInfo);
                }
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> removeBatch(String groupId, String idstr) {
        if (idstr != null && idstr.length() != 0) {
            List<String> ids = Arrays.asList(idstr.split(","));
            for (String id : ids) {
                XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(id));
                if (xxlJobInfo != null) {
                    xxlJobInfoDao.delete(Integer.parseInt(id));
                    xxlJobLogDao.delete(Integer.parseInt(id));
                    xxlJobLogGlueDao.deleteByJobId(Integer.parseInt(id));
                }
            }
        }
        return ReturnT.SUCCESS;
    }


}
