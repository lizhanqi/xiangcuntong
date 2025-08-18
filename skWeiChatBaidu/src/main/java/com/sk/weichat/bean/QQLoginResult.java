package com.sk.weichat.bean;

public class QQLoginResult {
    private long ret;
    private String accessToken;
    private String msg;
    private String pfkey;
    private String payToken;
    private String openid;
    private String pf;
    private long queryAuthorityCost;
    private long expiresIn;
    private long loginCost;
    private long expiresTime;
    private long authorityCost;

    /**
     * 第三方首次登陆免注册，服务端需要的参数
     */
    private String nickName;
    private int sex;
    private String headImageUrl;

    public long getRet() {
        return ret;
    }

    public void setRet(long ret) {
        this.ret = ret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPfkey() {
        return pfkey;
    }

    public void setPfkey(String pfkey) {
        this.pfkey = pfkey;
    }

    public String getPayToken() {
        return payToken;
    }

    public void setPayToken(String payToken) {
        this.payToken = payToken;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getPf() {
        return pf;
    }

    public void setPf(String pf) {
        this.pf = pf;
    }

    public long getQueryAuthorityCost() {
        return queryAuthorityCost;
    }

    public void setQueryAuthorityCost(long queryAuthorityCost) {
        this.queryAuthorityCost = queryAuthorityCost;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getLoginCost() {
        return loginCost;
    }

    public void setLoginCost(long loginCost) {
        this.loginCost = loginCost;
    }

    public long getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(long expiresTime) {
        this.expiresTime = expiresTime;
    }

    public long getAuthorityCost() {
        return authorityCost;
    }

    public void setAuthorityCost(long authorityCost) {
        this.authorityCost = authorityCost;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getHeadImageUrl() {
        return headImageUrl;
    }

    public void setHeadImageUrl(String headImageUrl) {
        this.headImageUrl = headImageUrl;
    }

    @Override
    public String toString() {
        return
                "QQLoginResult{" +
                        "ret = '" + ret + '\'' +
                        ",access_token = '" + accessToken + '\'' +
                        ",msg = '" + msg + '\'' +
                        ",pfkey = '" + pfkey + '\'' +
                        ",pay_token = '" + payToken + '\'' +
                        ",openid = '" + openid + '\'' +
                        ",pf = '" + pf + '\'' +
                        ",query_authority_cost = '" + queryAuthorityCost + '\'' +
                        ",expires_in = '" + expiresIn + '\'' +
                        ",login_cost = '" + loginCost + '\'' +
                        ",expires_time = '" + expiresTime + '\'' +
                        ",authority_cost = '" + authorityCost + '\'' +
                        "}";
    }
}
