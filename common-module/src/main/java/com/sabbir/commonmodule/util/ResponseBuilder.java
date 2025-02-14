package com.sabbir.commonmodule.util;

import com.sabbir.commonmodule.model.CustomHttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.sabbir.commonmodule.constant.CommonConstant.ERROR_CODE;
import static com.sabbir.commonmodule.constant.CommonConstant.ERROR_MESSAGE;

public class ResponseBuilder {

    public static ResponseEntity<CustomHttpResponse> buildSuccessResponse(HttpStatus httpStatus,
                                                                          Map<String, Object> responseBody){
        CustomHttpResponse successResponse = CustomHttpResponse
                .builder()
                .httpStatus(httpStatus)
                .responseBody(responseBody)
                .build();
        return new ResponseEntity<>(successResponse, httpStatus);
    }

    public static ResponseEntity<CustomHttpResponse> buildFailureResponse(HttpStatus httpStatus,
                                                           Integer errorCode, String errorMessage){
        CustomHttpResponse errorResponse = CustomHttpResponse
                .builder()
                .httpStatus(httpStatus)
                .errorBody(Map.of(ERROR_CODE, errorCode, ERROR_MESSAGE, errorMessage))
                .build();
        return new ResponseEntity<>(errorResponse, httpStatus);
    }
}
