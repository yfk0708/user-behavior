package com.djt.key;

import scala.math.Ordered;

import java.io.Serializable;

public class UserHourPackageKey implements Ordered<UserHourPackageKey>, Serializable {
    private long userId;
    private String hour;
    private String packageName;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


    @Override
    public int compare(UserHourPackageKey that) {
        return compareTo(that);
    }

    @Override
    public boolean $less(UserHourPackageKey that) {
        if (this.compareTo(that) < 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean $greater(UserHourPackageKey that) {
        if (this.compareTo(that) > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean $less$eq(UserHourPackageKey that) {
        if (this.compareTo(that) <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean $greater$eq(UserHourPackageKey that) {
        if (this.compareTo(that) >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(UserHourPackageKey that) {
        if (userId == that.getUserId()) {
            if (hour.compareTo(that.getHour()) == 0) {
                return packageName.compareTo(that.getPackageName());
            } else {
                return hour.compareTo(that.getHour());
            }
        } else {
            long n = userId - that.getUserId();
            if (n > 0) {
                return 1;
            } else {
                if (n == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }
}
