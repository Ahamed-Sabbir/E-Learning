package com.sabbir.commonmodule.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // only not null fields will pass
public class CustomHttpResponse {
    private HttpStatus httpStatus;
    private Map<String, Object> responseBody;
    private Map<String, Object> errorBody;
}
