package com.sabbir.commonmodule.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbir.commonmodule.exception.ApiException;
import com.sabbir.commonmodule.model.CustomHttpRequest;
import com.sabbir.commonmodule.model.CustomHttpResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.sabbir.commonmodule.constant.CommonConstant.*;

@Component
public class HttpCallLogic {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Logger systemLogger;

    public HttpCallLogic(RestTemplate restTemplate, ObjectMapper objectMapper, @Qualifier("systemLogger") Logger systemLogger) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.systemLogger = systemLogger;
    }

    public Map<String, Object> getHttpResponse(CustomHttpRequest customHttpRequest) {
        CustomHttpResponse customHttpResponse = executeRequest(customHttpRequest).getBody();
        if (customHttpResponse != null && customHttpResponse.getResponseBody() != null) {
            return customHttpResponse.getResponseBody();
        }
        return new HashMap<>();
    }

    public Map<String, Object> getHttpResponseWithException(CustomHttpRequest customHttpRequest) {
        CustomHttpResponse customHttpResponse = executeRequest(customHttpRequest).getBody();
        if (customHttpResponse != null && customHttpResponse.getResponseBody() != null) {
            return customHttpResponse.getResponseBody();
        } else if (customHttpResponse != null && customHttpResponse.getErrorBody() != null) {
            HttpStatus httpStatus = customHttpResponse.getHttpStatus();
            String errorCode = customHttpResponse.getErrorBody().get(ERROR_CODE).toString();
            String errorMessage = customHttpResponse.getErrorBody().get(ERROR_MESSAGE).toString();
            throw new ApiException(httpStatus, errorCode, errorMessage);
        }
        return new HashMap<>();
    }

    public ResponseEntity<CustomHttpResponse> executeRequest(CustomHttpRequest customHttpRequest) {
        try {
            HttpMethod httpMethod = customHttpRequest.getMethodType();
            URI uri = prepareRequestUri(customHttpRequest);
            HttpHeaders requestHeaders = prepareRequestHeaders(customHttpRequest);
            Map<String, ?> requestBody = prepareRequestBody(customHttpRequest);
            HttpEntity<Map<String, ?>> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
            return restTemplate.exchange(uri, httpMethod, requestEntity, CustomHttpResponse.class);
        }catch (HttpClientErrorException | HttpServerErrorException ex){
            return handle4xx5xxErrorResponse(ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            String errorMessage = "Error occurred while executing HTTP request! Reason: " + ex.getMessage();
            systemLogger.error(errorMessage);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "500", errorMessage);
        }
    }

    private ResponseEntity<CustomHttpResponse> handle4xx5xxErrorResponse(HttpStatusCode httpStatusCode,
                                                                         String errorResponseBody) {
        try {
            CustomHttpResponse customHttpResponse = objectMapper.readValue(errorResponseBody, CustomHttpResponse.class);
            if (customHttpResponse.getHttpStatus() == null || customHttpResponse.getErrorBody() == null) {
                Map errorBody = objectMapper.readValue(errorResponseBody, Map.class);
                Integer status = (Integer) errorBody.get("status");
                String error = (String) errorBody.get("error");
                String path = (String) errorBody.get("path");
                customHttpResponse.setHttpStatus(HttpStatus.valueOf(status));
                customHttpResponse.setErrorBody(Map.of(ERROR_CODE, status, ERROR_MESSAGE, error + " - " + path));
            }
            return new ResponseEntity<>(customHttpResponse, httpStatusCode);
        } catch (JsonProcessingException ex) {
            String errorMessage = "Error occurred while handling 4xx/5xx response! Reason: " + ex.getMessage();
            systemLogger.error(errorMessage);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "500", errorMessage);
        }
    }

    public ResponseEntity<byte[]> fetchMediaContent(CustomHttpRequest customHttpRequest) {
        URI url;
        try {
            url = prepareRequestUri(customHttpRequest);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpMethod methodType = customHttpRequest.getMethodType();
        HttpHeaders requestHeaders = prepareRequestHeaders(customHttpRequest);
        HttpEntity<Map<String, ?>> requestEntity = new HttpEntity<>(requestHeaders);
        return restTemplate.exchange(url, methodType, requestEntity, byte[].class);
    }

    private URI prepareRequestUri(CustomHttpRequest customHttpRequest) throws URISyntaxException {
        String url = customHttpRequest.getUrl();
        Map<String, String> queryParameterMap = customHttpRequest.getQueryParameterMap();
        if (queryParameterMap != null && !queryParameterMap.isEmpty()) {
            String queryParameters = buildQueryParameters(queryParameterMap);
            return new URI(url + "?" + queryParameters);
        }
        return new URI(url);
    }
    
    private String buildQueryParameters(Map<String, String> queryParameterMap) {
        StringBuilder queryParameters = new StringBuilder();
        for (Map.Entry<String, String> queryParameter : queryParameterMap.entrySet()) {
            queryParameters
                    .append(queryParameter.getKey())
                    .append("=")
                    .append(queryParameter.getValue())
                    .append("&");
        }
        return queryParameters.substring(0, queryParameters.length() - 1);
    }
    
    private HttpHeaders prepareRequestHeaders(CustomHttpRequest customHttpRequest) {
        Map<String, String> headerParameterMap = customHttpRequest.getHeaderParameterMap();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(REQUEST_ID, customHttpRequest.getRequestId());
        if (headerParameterMap != null && !headerParameterMap.isEmpty()) {
            for (Map.Entry<String, String> header : headerParameterMap.entrySet()) {
                httpHeaders.add(header.getKey(), header.getValue());
            }
        }
        return httpHeaders;
    }

    private Map<String, ?> prepareRequestBody(CustomHttpRequest customHttpRequest) {
        Map<String, Object> bodyParameterMap = customHttpRequest.getBodyParameterMap();
        if (bodyParameterMap != null && !bodyParameterMap.isEmpty()) {
            if (isMultipartFormDataHeaderPresent(customHttpRequest)) {
                return prepareMultiValueRequestBody(bodyParameterMap);
            }
            return bodyParameterMap;
        }
        return new HashMap<>();
    }
    private boolean isMultipartFormDataHeaderPresent(CustomHttpRequest customHttpRequest) {
        Map<String, String> headerParameterMap = customHttpRequest.getHeaderParameterMap();
        if (headerParameterMap != null && headerParameterMap.containsKey(CONTENT_TYPE_HEADER_KEY)) {
            return headerParameterMap.get(CONTENT_TYPE_HEADER_KEY).equals(MediaType.MULTIPART_FORM_DATA_VALUE);
        }
        return false;
    }

    private MultiValueMap<String, Object> prepareMultiValueRequestBody(Map<String, Object> bodyParameterMap) {
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> bodyParameter : bodyParameterMap.entrySet()) {
            if (bodyParameter.getValue() instanceof MultipartFile[] multipartFiles) {
                for (MultipartFile multipartFile : multipartFiles) {
                    requestBody.add(bodyParameter.getKey(), multipartFile.getResource());
                }
            } else {
                requestBody.add(bodyParameter.getKey(), bodyParameter.getValue());
            }
        }
        return requestBody;
    }
}
