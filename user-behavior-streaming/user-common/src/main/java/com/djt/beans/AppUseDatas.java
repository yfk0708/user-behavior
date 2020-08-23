package com.djt.beans;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

/**
 * 实体类
 * APP使用情况数据
 *[{"package":"com.browser1","activetime":60000},{"package":"com.browser3","activetime":120000}]
 */
public class AppUseDatas implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("package")
    private String packageName;

    @JsonProperty("activetime")
    private long activeTime;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }
}
