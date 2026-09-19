package com.eduagent.learning.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void onboardRejectsBlankMessage() {
        OnboardRequest request = new OnboardRequest();
        request.setMessage("  ");
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void studyLogRejectsInvalidInput() {
        StudyLogRequest request = new StudyLogRequest();
        request.setModule("");
        request.setDurationSec(0);
        assertEquals(2, validator.validate(request).size());
    }

    @Test
    void updateTaskRequiresCompletedButAllowsTaskIdOnly() {
        UpdateTaskRequest invalid = new UpdateTaskRequest();
        invalid.setTaskId(1L);
        assertEquals(1, validator.validate(invalid).size());

        UpdateTaskRequest valid = new UpdateTaskRequest();
        valid.setTaskId(1L);
        valid.setCompleted(true);
        assertTrue(validator.validate(valid).isEmpty());
    }

    @Test
    void bindClassRequiresPositiveId() {
        BindClassRequest request = new BindClassRequest();
        request.setClassId(0L);
        assertEquals(1, validator.validate(request).size());
    }
}
