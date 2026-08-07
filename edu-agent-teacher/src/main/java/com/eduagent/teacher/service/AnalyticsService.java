package com.eduagent.teacher.service;

import com.eduagent.teacher.vo.ClassAnalyticsVO;
import com.eduagent.teacher.vo.ClassOverviewVO;

public interface AnalyticsService {

    ClassAnalyticsVO classAnalytics(Long classId);

    ClassOverviewVO classOverview(Long classId);
}
