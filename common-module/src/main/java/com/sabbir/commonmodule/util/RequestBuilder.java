package com.sabbir.commonmodule.util;

import com.sabbir.commonmodule.model.CustomHttpRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static com.sabbir.commonmodule.constant.CommonConstant.REQUEST_ID;

/**
 * This RequestBuilder is needed to make request from -
 * one microservice to another microservice
 */

public class RequestBuilder {

    private static final String LOADBALANCER_PREFIX = "lb://";

    public static CustomHttpRequest buildRequest(HttpMethod methodType, String url,
                                                 Map<String, String> headerParameterMap,
                                                 Map<String, String> queryParameterMap,
                                                 Map<String, Object> bodyParameterMap) {
        return CustomHttpRequest
                .builder()
                .requestId(MDC.get(REQUEST_ID))
                .methodType(methodType)
                .url(LOADBALANCER_PREFIX + url)
                .headerParameterMap(headerParameterMap)
                .queryParameterMap(queryParameterMap)
                .bodyParameterMap(bodyParameterMap)
                .build();
    }
}
