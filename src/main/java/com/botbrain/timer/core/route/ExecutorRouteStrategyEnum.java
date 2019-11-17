package com.botbrain.timer.core.route;

import com.botbrain.timer.core.route.strategy.*;
import com.botbrain.timer.core.util.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by xuxueli on 17/3/10.
 */
public enum ExecutorRouteStrategyEnum {

    FIRST(i18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),
    LAST(i18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),
    ROUND(i18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),
    RANDOM(i18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),
    CONSISTENT_HASH(i18nUtil.getString("jobconf_route_consistenthash"), new ExecutorRouteConsistentHash()),
    LEAST_FREQUENTLY_USED(i18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),
    LEAST_RECENTLY_USED(i18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),
    FAILOVER(i18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),
    BUSYOVER(i18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyover()),
    SHARDING_BROADCAST(getI().getString("jobconf_route_shard"), null);

    ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
        this.title = title;
        this.router = router;
    }

    private  I18nUtil i18nUtil=new I18nUtil();
    private static I18nUtil getI(){
        return i18nUtil;
    }
    private String title;
    private ExecutorRouter router;

    public String getTitle() {
        return title;
    }
    public ExecutorRouter getRouter() {
        return router;
    }

    public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem){
        if (name != null) {
            for (ExecutorRouteStrategyEnum item: ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }

}
