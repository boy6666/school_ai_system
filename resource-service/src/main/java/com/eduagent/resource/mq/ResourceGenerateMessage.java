package com.eduagent.resource.mq;

import com.eduagent.resource.dto.ResourceGenerateReq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceGenerateMessage implements Serializable {
    private Long resourceId;
    private Long userId;
    private ResourceGenerateReq req;
}
