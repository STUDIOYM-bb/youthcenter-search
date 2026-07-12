package com.themoa.youthcentersearch.youthcenter.dto.external;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthCenterPolicyRaw {
    private String plcyNo;
    private String plcyNm;
    private String plcyKywdNm;
    private String plcyExplnCn;
    private String lclsfNm;
    private String mclsfNm;
    private String plcySprtCn;
    private String sprvsnInstCd;
    private String sprvsnInstCdNm;
    private String operInstCd;
    private String operInstCdNm;
    private String aplyPrdSeCd;
    private String bizPrdSeCd;
    private String bizPrdBgngYmd;
    private String bizPrdEndYmd;
    private String bizPrdEtcCn;
    private String plcyAplyMthdCn;
    private String srngMthdCn;
    private String aplyUrlAddr;
    private String sbmsnDcmntCn;
    private String etcMttrCn;
    private String refUrlAddr1;
    private String refUrlAddr2;
    private String sprtTrgtMinAge;
    private String sprtTrgtMaxAge;
    private String sprtTrgtAgeLmtYn;
    private String mrgSttsCd;
    private String earnCndSeCd;
    private String earnMinAmt;
    private String earnMaxAmt;
    private String earnEtcCn;
    private String addAplyQlfcCndCn;
    private String ptcpPrpTrgtCn;
    private String zipCd;
    private String plcyMajorCd;
    private String jobCd;
    private String schoolCd;
    private String aplyYmd;
    private String frstRegDt;
    private String lastMdfcnDt;
    private String sbizCd;
    private final Map<String, Object> additionalFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditionalField(String name, Object value) {
        additionalFields.put(name, value);
    }

    public Map<String, Object> toFieldMap() {
        Map<String, Object> fields = new LinkedHashMap<>(additionalFields);
        put(fields, "plcyNo", plcyNo);
        put(fields, "plcyNm", plcyNm);
        put(fields, "plcyKywdNm", plcyKywdNm);
        put(fields, "plcyExplnCn", plcyExplnCn);
        put(fields, "lclsfNm", lclsfNm);
        put(fields, "mclsfNm", mclsfNm);
        put(fields, "plcySprtCn", plcySprtCn);
        put(fields, "sprvsnInstCd", sprvsnInstCd);
        put(fields, "sprvsnInstCdNm", sprvsnInstCdNm);
        put(fields, "operInstCd", operInstCd);
        put(fields, "operInstCdNm", operInstCdNm);
        put(fields, "aplyPrdSeCd", aplyPrdSeCd);
        put(fields, "bizPrdSeCd", bizPrdSeCd);
        put(fields, "bizPrdBgngYmd", bizPrdBgngYmd);
        put(fields, "bizPrdEndYmd", bizPrdEndYmd);
        put(fields, "bizPrdEtcCn", bizPrdEtcCn);
        put(fields, "plcyAplyMthdCn", plcyAplyMthdCn);
        put(fields, "srngMthdCn", srngMthdCn);
        put(fields, "aplyUrlAddr", aplyUrlAddr);
        put(fields, "sbmsnDcmntCn", sbmsnDcmntCn);
        put(fields, "etcMttrCn", etcMttrCn);
        put(fields, "refUrlAddr1", refUrlAddr1);
        put(fields, "refUrlAddr2", refUrlAddr2);
        put(fields, "sprtTrgtMinAge", sprtTrgtMinAge);
        put(fields, "sprtTrgtMaxAge", sprtTrgtMaxAge);
        put(fields, "sprtTrgtAgeLmtYn", sprtTrgtAgeLmtYn);
        put(fields, "mrgSttsCd", mrgSttsCd);
        put(fields, "earnCndSeCd", earnCndSeCd);
        put(fields, "earnMinAmt", earnMinAmt);
        put(fields, "earnMaxAmt", earnMaxAmt);
        put(fields, "earnEtcCn", earnEtcCn);
        put(fields, "addAplyQlfcCndCn", addAplyQlfcCndCn);
        put(fields, "ptcpPrpTrgtCn", ptcpPrpTrgtCn);
        put(fields, "zipCd", zipCd);
        put(fields, "plcyMajorCd", plcyMajorCd);
        put(fields, "jobCd", jobCd);
        put(fields, "schoolCd", schoolCd);
        put(fields, "aplyYmd", aplyYmd);
        put(fields, "frstRegDt", frstRegDt);
        put(fields, "lastMdfcnDt", lastMdfcnDt);
        put(fields, "sbizCd", sbizCd);
        return fields;
    }

    private static void put(Map<String, Object> fields, String key, Object value) {
        fields.put(key, value);
    }

    public String getPlcyNo() { return plcyNo; }
    public void setPlcyNo(String plcyNo) { this.plcyNo = plcyNo; }
    public String getPlcyNm() { return plcyNm; }
    public void setPlcyNm(String plcyNm) { this.plcyNm = plcyNm; }
    public String getPlcyKywdNm() { return plcyKywdNm; }
    public void setPlcyKywdNm(String plcyKywdNm) { this.plcyKywdNm = plcyKywdNm; }
    public String getPlcyExplnCn() { return plcyExplnCn; }
    public void setPlcyExplnCn(String plcyExplnCn) { this.plcyExplnCn = plcyExplnCn; }
    public String getLclsfNm() { return lclsfNm; }
    public void setLclsfNm(String lclsfNm) { this.lclsfNm = lclsfNm; }
    public String getMclsfNm() { return mclsfNm; }
    public void setMclsfNm(String mclsfNm) { this.mclsfNm = mclsfNm; }
    public String getPlcySprtCn() { return plcySprtCn; }
    public void setPlcySprtCn(String plcySprtCn) { this.plcySprtCn = plcySprtCn; }
    public String getSprvsnInstCd() { return sprvsnInstCd; }
    public void setSprvsnInstCd(String sprvsnInstCd) { this.sprvsnInstCd = sprvsnInstCd; }
    public String getSprvsnInstCdNm() { return sprvsnInstCdNm; }
    public void setSprvsnInstCdNm(String sprvsnInstCdNm) { this.sprvsnInstCdNm = sprvsnInstCdNm; }
    public String getOperInstCd() { return operInstCd; }
    public void setOperInstCd(String operInstCd) { this.operInstCd = operInstCd; }
    public String getOperInstCdNm() { return operInstCdNm; }
    public void setOperInstCdNm(String operInstCdNm) { this.operInstCdNm = operInstCdNm; }
    public String getAplyPrdSeCd() { return aplyPrdSeCd; }
    public void setAplyPrdSeCd(String aplyPrdSeCd) { this.aplyPrdSeCd = aplyPrdSeCd; }
    public String getBizPrdSeCd() { return bizPrdSeCd; }
    public void setBizPrdSeCd(String bizPrdSeCd) { this.bizPrdSeCd = bizPrdSeCd; }
    public String getBizPrdBgngYmd() { return bizPrdBgngYmd; }
    public void setBizPrdBgngYmd(String bizPrdBgngYmd) { this.bizPrdBgngYmd = bizPrdBgngYmd; }
    public String getBizPrdEndYmd() { return bizPrdEndYmd; }
    public void setBizPrdEndYmd(String bizPrdEndYmd) { this.bizPrdEndYmd = bizPrdEndYmd; }
    public String getBizPrdEtcCn() { return bizPrdEtcCn; }
    public void setBizPrdEtcCn(String bizPrdEtcCn) { this.bizPrdEtcCn = bizPrdEtcCn; }
    public String getPlcyAplyMthdCn() { return plcyAplyMthdCn; }
    public void setPlcyAplyMthdCn(String plcyAplyMthdCn) { this.plcyAplyMthdCn = plcyAplyMthdCn; }
    public String getSrngMthdCn() { return srngMthdCn; }
    public void setSrngMthdCn(String srngMthdCn) { this.srngMthdCn = srngMthdCn; }
    public String getAplyUrlAddr() { return aplyUrlAddr; }
    public void setAplyUrlAddr(String aplyUrlAddr) { this.aplyUrlAddr = aplyUrlAddr; }
    public String getSbmsnDcmntCn() { return sbmsnDcmntCn; }
    public void setSbmsnDcmntCn(String sbmsnDcmntCn) { this.sbmsnDcmntCn = sbmsnDcmntCn; }
    public String getEtcMttrCn() { return etcMttrCn; }
    public void setEtcMttrCn(String etcMttrCn) { this.etcMttrCn = etcMttrCn; }
    public String getRefUrlAddr1() { return refUrlAddr1; }
    public void setRefUrlAddr1(String refUrlAddr1) { this.refUrlAddr1 = refUrlAddr1; }
    public String getRefUrlAddr2() { return refUrlAddr2; }
    public void setRefUrlAddr2(String refUrlAddr2) { this.refUrlAddr2 = refUrlAddr2; }
    public String getSprtTrgtMinAge() { return sprtTrgtMinAge; }
    public void setSprtTrgtMinAge(String sprtTrgtMinAge) { this.sprtTrgtMinAge = sprtTrgtMinAge; }
    public String getSprtTrgtMaxAge() { return sprtTrgtMaxAge; }
    public void setSprtTrgtMaxAge(String sprtTrgtMaxAge) { this.sprtTrgtMaxAge = sprtTrgtMaxAge; }
    public String getSprtTrgtAgeLmtYn() { return sprtTrgtAgeLmtYn; }
    public void setSprtTrgtAgeLmtYn(String sprtTrgtAgeLmtYn) { this.sprtTrgtAgeLmtYn = sprtTrgtAgeLmtYn; }
    public String getMrgSttsCd() { return mrgSttsCd; }
    public void setMrgSttsCd(String mrgSttsCd) { this.mrgSttsCd = mrgSttsCd; }
    public String getEarnCndSeCd() { return earnCndSeCd; }
    public void setEarnCndSeCd(String earnCndSeCd) { this.earnCndSeCd = earnCndSeCd; }
    public String getEarnMinAmt() { return earnMinAmt; }
    public void setEarnMinAmt(String earnMinAmt) { this.earnMinAmt = earnMinAmt; }
    public String getEarnMaxAmt() { return earnMaxAmt; }
    public void setEarnMaxAmt(String earnMaxAmt) { this.earnMaxAmt = earnMaxAmt; }
    public String getEarnEtcCn() { return earnEtcCn; }
    public void setEarnEtcCn(String earnEtcCn) { this.earnEtcCn = earnEtcCn; }
    public String getAddAplyQlfcCndCn() { return addAplyQlfcCndCn; }
    public void setAddAplyQlfcCndCn(String addAplyQlfcCndCn) { this.addAplyQlfcCndCn = addAplyQlfcCndCn; }
    public String getPtcpPrpTrgtCn() { return ptcpPrpTrgtCn; }
    public void setPtcpPrpTrgtCn(String ptcpPrpTrgtCn) { this.ptcpPrpTrgtCn = ptcpPrpTrgtCn; }
    public String getZipCd() { return zipCd; }
    public void setZipCd(String zipCd) { this.zipCd = zipCd; }
    public String getPlcyMajorCd() { return plcyMajorCd; }
    public void setPlcyMajorCd(String plcyMajorCd) { this.plcyMajorCd = plcyMajorCd; }
    public String getJobCd() { return jobCd; }
    public void setJobCd(String jobCd) { this.jobCd = jobCd; }
    public String getSchoolCd() { return schoolCd; }
    public void setSchoolCd(String schoolCd) { this.schoolCd = schoolCd; }
    public String getAplyYmd() { return aplyYmd; }
    public void setAplyYmd(String aplyYmd) { this.aplyYmd = aplyYmd; }
    public String getFrstRegDt() { return frstRegDt; }
    public void setFrstRegDt(String frstRegDt) { this.frstRegDt = frstRegDt; }
    public String getLastMdfcnDt() { return lastMdfcnDt; }
    public void setLastMdfcnDt(String lastMdfcnDt) { this.lastMdfcnDt = lastMdfcnDt; }
    public String getSbizCd() { return sbizCd; }
    public void setSbizCd(String sbizCd) { this.sbizCd = sbizCd; }
    public Map<String, Object> getAdditionalFields() { return additionalFields; }
}
