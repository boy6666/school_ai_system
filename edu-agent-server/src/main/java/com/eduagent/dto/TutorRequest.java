package com.eduagent.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class TutorRequest {
    @NotBlank
    private String message;
}
