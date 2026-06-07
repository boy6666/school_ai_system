from typing import List, Optional

from pydantic import BaseModel, Field


class StudentProfile(BaseModel):
    major: str = ""
    grade: str = ""
    course: str = "数据结构"
    topic: str = "递归"
    learning_goal: str = "掌握当前知识点"
    knowledge_base: str = ""
    cognitive_style: str = "偏好结构化讲解"
    weaknesses: List[str] = Field(default_factory=list)
    mistake_patterns: List[str] = Field(default_factory=list)
    resource_preference: List[str] = Field(default_factory=list)
    pace: str = ""
    last_updated: Optional[str] = None
