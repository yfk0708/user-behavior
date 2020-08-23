package com.djt.beans;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 实体类
 * 用户提交的数据
 * {"userId":1000,"day":"2020-03-07","begintime":1546654200000,"endtime":1546654800000,"data":
 *[{"package":"com.browser1","activetime":60000},{"package":"com.browser3","activetime":120000}]}
 */
public class UserBehaviorLog implements Serializable {
    private long userId;

    private String day;
    //	begintime和endtime为毫秒格式
    @JsonProperty("begintime")
    private long beginTime;

    @JsonProperty("endtime")
    private long endTime;

    @JsonProperty("data")
    private List<AppUseDatas> appUseDataList;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<AppUseDatas> getAppUseDataList() {
        return appUseDataList;
    }

    public void setAppUseDataList(List<AppUseDatas> appUseDataList) {
        this.appUseDataList = appUseDataList;
    }
}
