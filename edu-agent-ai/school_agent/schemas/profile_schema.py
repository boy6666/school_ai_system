from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


# 每个维度的三层次专属标签
DIM_LEVEL_LABELS = {
    "knowledge_mastery":     {"level_1": "了解概念", "level_2": "熟练应用", "level_3": "深入精通"},
    "learning_goal_clarity": {"level_1": "方向模糊", "level_2": "目标明确", "level_3": "系统规划"},
    "cognitive_adaptation":  {"level_1": "有待观察", "level_2": "初显偏好", "level_3": "策略自驱"},
    "mistake_avoidance":     {"level_1": "易重复错", "level_2": "能自查纠", "level_3": "主动预防"},
    "learning_autonomy":     {"level_1": "被动等待", "level_2": "主动提问", "level_3": "自主深耕"},
    "overall_level":         {"level_1": "基础补齐", "level_2": "稳步提升", "level_3": "拔尖拓展"},
}
_FALLBACK_LABELS = {"level_1": "入门", "level_2": "熟练", "level_3": "精通"}


class DimensionLevel(str, Enum):
    LEVEL_1 = "level_1"
    LEVEL_2 = "level_2"
    LEVEL_3 = "level_3"

    @classmethod
    def label(cls, level: "DimensionLevel", dim_name: str = "") -> str:
        labels = DIM_LEVEL_LABELS.get(dim_name, _FALLBACK_LABELS)
        return labels.get(str(level), "入门")

    @classmethod
    def next_level(cls, level: "DimensionLevel") -> Optional["DimensionLevel"]:
        order = {cls.LEVEL_1: cls.LEVEL_2, cls.LEVEL_2: cls.LEVEL_3}
        return order.get(level)


class DimensionState(BaseModel):
    level: DimensionLevel = DimensionLevel.LEVEL_1
    score: int = Field(default=30, ge=0, le=100)
    evidence: List[str] = Field(default_factory=list)
    last_updated: Optional[str] = None


class StudentProfile(BaseModel):
    # 六维层次化画像
    knowledge_mastery: DimensionState = Field(
        default_factory=DimensionState, description="知识掌握度"
    )
    learning_goal_clarity: DimensionState = Field(
        default_factory=DimensionState, description="学习目标清晰度"
    )
    cognitive_adaptation: DimensionState = Field(
        default_factory=DimensionState, description="认知风格适配"
    )
    mistake_avoidance: DimensionState = Field(
        default_factory=DimensionState, description="错误规避力"
    )
    learning_autonomy: DimensionState = Field(
        default_factory=DimensionState, description="学习自主性"
    )
    overall_level: DimensionState = Field(
        default_factory=DimensionState, description="综合能力"
    )

    # 辅助信息
    major: str = "计算机相关专业"
    grade: str = "未知年级"
    course: str = "数据结构"
    topic: str = "Java基础"
    learning_goal: str = ""
    knowledge_base: str = ""
    current_mastery: str = ""
    cognitive_style: str = ""
    weaknesses: List[str] = Field(default_factory=list)
    mistake_patterns: List[str] = Field(default_factory=list)
    learning_behavior: str = ""
    resource_preference: List[str] = Field(default_factory=list)
    pace: str = "中速"
    overall_type: Optional[str] = None
    profile_suggestions: List[str] = Field(default_factory=list)
    conversation_count: int = 0
    created_at: Optional[str] = None
    last_updated: Optional[str] = None

    def get_dimension(self, name: str) -> DimensionState:
        dims = {
            "knowledge_mastery": self.knowledge_mastery,
            "learning_goal_clarity": self.learning_goal_clarity,
            "cognitive_adaptation": self.cognitive_adaptation,
            "mistake_avoidance": self.mistake_avoidance,
            "learning_autonomy": self.learning_autonomy,
            "overall_level": self.overall_level,
        }
        return dims.get(name, DimensionState())

    def set_dimension(self, name: str, state: DimensionState) -> None:
        if name == "knowledge_mastery":
            self.knowledge_mastery = state
        elif name == "learning_goal_clarity":
            self.learning_goal_clarity = state
        elif name == "cognitive_adaptation":
            self.cognitive_adaptation = state
        elif name == "mistake_avoidance":
            self.mistake_avoidance = state
        elif name == "learning_autonomy":
            self.learning_autonomy = state
        elif name == "overall_level":
            self.overall_level = state

    def to_dict(self) -> Dict[str, Any]:
        return {
            "knowledge_mastery": {
                "level": self.knowledge_mastery.level.value,
                "score": self.knowledge_mastery.score,
                "evidence": self.knowledge_mastery.evidence,
                "last_updated": self.knowledge_mastery.last_updated,
            },
            "learning_goal_clarity": {
                "level": self.learning_goal_clarity.level.value,
                "score": self.learning_goal_clarity.score,
                "evidence": self.learning_goal_clarity.evidence,
                "last_updated": self.learning_goal_clarity.last_updated,
            },
            "cognitive_adaptation": {
                "level": self.cognitive_adaptation.level.value,
                "score": self.cognitive_adaptation.score,
                "evidence": self.cognitive_adaptation.evidence,
                "last_updated": self.cognitive_adaptation.last_updated,
            },
            "mistake_avoidance": {
                "level": self.mistake_avoidance.level.value,
                "score": self.mistake_avoidance.score,
                "evidence": self.mistake_avoidance.evidence,
                "last_updated": self.mistake_avoidance.last_updated,
            },
            "learning_autonomy": {
                "level": self.learning_autonomy.level.value,
                "score": self.learning_autonomy.score,
                "evidence": self.learning_autonomy.evidence,
                "last_updated": self.learning_autonomy.last_updated,
            },
            "overall_level": {
                "level": self.overall_level.level.value,
                "score": self.overall_level.score,
                "evidence": self.overall_level.evidence,
                "last_updated": self.overall_level.last_updated,
            },
            "major": self.major,
            "grade": self.grade,
            "course": self.course,
            "topic": self.topic,
            "learning_goal": self.learning_goal,
            "knowledge_base": self.knowledge_base,
            "current_mastery": self.current_mastery,
            "cognitive_style": self.cognitive_style,
            "weaknesses": self.weaknesses,
            "mistake_patterns": self.mistake_patterns,
            "learning_behavior": self.learning_behavior,
            "resource_preference": self.resource_preference,
            "pace": self.pace,
            "overall_type": self.overall_type,
            "profile_suggestions": self.profile_suggestions,
            "conversation_count": self.conversation_count,
            "created_at": self.created_at,
            "last_updated": self.last_updated,
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "StudentProfile":
        def parse_dim(key: str) -> DimensionState:
            dim_data = data.get(key)
            if isinstance(dim_data, dict):
                return DimensionState(
                    level=DimensionLevel(dim_data.get("level", "level_1")),
                    score=dim_data.get("score", 30),
                    evidence=dim_data.get("evidence", []),
                    last_updated=dim_data.get("last_updated"),
                )
            return DimensionState()

        return cls(
            knowledge_mastery=parse_dim("knowledge_mastery"),
            learning_goal_clarity=parse_dim("learning_goal_clarity"),
            cognitive_adaptation=parse_dim("cognitive_adaptation"),
            mistake_avoidance=parse_dim("mistake_avoidance"),
            learning_autonomy=parse_dim("learning_autonomy"),
            overall_level=parse_dim("overall_level"),
            major=data.get("major", "计算机相关专业"),
            grade=data.get("grade", "未知年级"),
            course=data.get("course", "数据结构"),
            topic=data.get("topic", "Java基础"),
            learning_goal=data.get("learning_goal", ""),
            knowledge_base=data.get("knowledge_base", ""),
            current_mastery=data.get("current_mastery", ""),
            cognitive_style=data.get("cognitive_style", ""),
            weaknesses=data.get("weaknesses", []),
            mistake_patterns=data.get("mistake_patterns", []),
            learning_behavior=data.get("learning_behavior", ""),
            resource_preference=data.get("resource_preference", []),
            pace=data.get("pace", "中速"),
            overall_type=data.get("overall_type"),
            profile_suggestions=data.get("profile_suggestions", []),
            conversation_count=data.get("conversation_count", 0),
            created_at=data.get("created_at"),
            last_updated=data.get("last_updated"),
        )
