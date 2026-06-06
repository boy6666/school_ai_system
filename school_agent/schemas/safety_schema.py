from typing import List

from pydantic import BaseModel, Field


class SafetyReport(BaseModel):
    stage: str
    passed: bool = True
    risk_level: str = "low"
    issues: List[str] = Field(default_factory=list)
