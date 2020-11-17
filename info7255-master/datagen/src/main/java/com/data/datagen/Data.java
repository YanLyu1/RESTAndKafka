/**
 * @Author Aditya Kelkar
 */


package com.data.datagen;

public class Data {
    private String AdsID;
    private String hostAddr;
    private String hostName;
    private String usrAgent;

    public Data(String AdsID, String hostAddr, String hostName, String usrAgent) {
        this.AdsID = AdsID;
        this.hostAddr = hostAddr;
        this.hostName = hostName;
        this.usrAgent = usrAgent;
    }

    public String getAdsID() {
        return AdsID;
    }

    public void setAdsID(String adsID) {
        this.AdsID = adsID;
    }

    public String getHostAddr() {
        return hostAddr;
    }

    public void setHostAddr(String hostAddr) {
        this.hostAddr = hostAddr;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUsrAgent() {
        return usrAgent;
    }

    public void setUsrAgent(String usrAgent) {
        this.usrAgent = usrAgent;
    }
}
