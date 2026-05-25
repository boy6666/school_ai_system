from typing import List, Optional

from pydantic import BaseModel, Field


class QuizItem(BaseModel):
    type: str
    difficulty: str = "基础"
    question: str
    options: Optional[List[str]] = None
    answer: str
    analysis: str = ""
