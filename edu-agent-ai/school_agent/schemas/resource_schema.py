from typing import Any, Dict, List

from pydantic import BaseModel, Field


class ResourcePackage(BaseModel):
    course_doc: str = ""
    mindmap: str = ""
    quiz: List[Dict[str, Any]] = Field(default_factory=list)
    extended_reading: str = ""
    code_practice: str = ""
    video_script: str = ""
