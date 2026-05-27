from typing import List, Optional

from pydantic import BaseModel, Field


class StudentProfile(BaseModel):
    major: str = "计算机相关专业"
    grade: str = "未知年级"
    course: str = "数据结构"
    topic: str = "递归"
    learning_goal: str = "掌握当前知识点"
    knowledge_base: str = "基础未知"
    current_mastery: str = ""
    cognitive_style: str = "偏好结构化讲解"
    weaknesses: List[str] = Field(default_factory=list)
    mistake_patterns: List[str] = Field(default_factory=list)
    learning_behavior: str = ""
    resource_preference: List[str] = Field(default_factory=list)
    pace: str = "中速"
    overall_type: Optional[str] = None
    last_updated: Optional[str] = None
