package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 缁戝畾涓撶敤 ObjectMapper锛氳泧褰㈠瓧娈点€佹嫆缁濇湭鐭ュ睘鎬с€佺渷鐣ユ湭鍑虹幇鐨勫彲閫?null銆?
 * 涓嶆敼鍙樼敓浜?diagnosis-service 鐨勫叏灞€ Jackson 閰嶇疆銆?
 */
public final class SharedContractsMapper {
    private SharedContractsMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }
}
