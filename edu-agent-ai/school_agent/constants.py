INTENT_CHAT = "chat"
INTENT_EXPLAIN = "explain"
INTENT_INIT = "init"
INTENT_ONBOARDING = "onboarding"
INTENT_QUIZ = "quiz"
INTENT_RETRIEVE = "retrieve"
INTENT_RESOURCE = "resource"
INTENT_TUTOR = "tutor"
INTENT_REJECT = "reject"

ALL_INTENTS = {
    INTENT_CHAT, INTENT_EXPLAIN, INTENT_INIT, INTENT_ONBOARDING,
    INTENT_QUIZ, INTENT_RETRIEVE, INTENT_RESOURCE, INTENT_TUTOR, INTENT_REJECT,
}

DEFAULT_STUDENT_ID = "student_001"
DEFAULT_SESSION_ID = "default_session"

RESOURCE_TYPES = [
    "course_doc", "mindmap", "quiz", "extended_reading", "code_practice", "video_script",
]
