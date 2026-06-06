from datetime import datetime
from typing import Optional

def log(step: str, msg: str) -> None:
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] {step:<8s} | {msg}")

def log_agent_start(name: str, prompt: str) -> None:
    log("AGENT", f"{name} start")
    preview = prompt.replace(chr(10), " ")
    log("PROMPT", f"{name}: {preview}")

def log_agent_done(name: str, elapsed: float, chars: int) -> None:
    log("AGENT", f"{name} done ({elapsed:.1f}s, {chars} chars)")

def log_silent(name: str, msg: str) -> None:
    log("SILENT", f"{name}: {msg}")

def log_route(decision: dict) -> None:
    action = decision.get("action", "?")
    silent = decision.get("silent_tasks", [])
    log("ROUTE", f"LLM decided: action={action}, silent={silent}")

def log_output(chars: int) -> None:
    log("OUTPUT", f"final_answer prepared ({chars} chars)")
