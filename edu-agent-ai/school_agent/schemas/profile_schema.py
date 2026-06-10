from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class DimensionLevel(str, Enum):
    LEVEL_1 = "level_1"
    LEVEL_2 = "level_2"
    LEVEL_3 = "level_3"

    @classmethod
    def label(cls, level: "DimensionLevel", dim_name: str = "") -> str:
        labels = {
            "level_1": "入门",
            "level_2": "熟练",
            "level_3": "精通",
        }
        return labels.get(level.value, "入门")


class DimensionState(BaseModel):
    score: int = 30
    level: str = "level_1"
    evidence: List[str] = Field(default_factory=list)

    def model_dump(self) -> Dict[str, Any]:
        return {
            "score": self.score,
            "level": self.level,
            "evidence": self.evidence,
        }


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
