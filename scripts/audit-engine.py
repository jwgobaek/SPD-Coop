"""List lexical dependency sites in tracked Java sources at the verified upstream revision."""
import re
import subprocess

revision = "57a4e06a4caf162446d1c28caa7983f0493fecf0"
groups = {
    "hero": r"\bDungeon\s*\.\s*hero\b",
    "world": r"\bDungeon\s*\.\s*(?:gold|energy|quickslot|level|depth|branch)\b",
    "vision": r"\b(?:heroFOV|visited|mapped|mindVisionEnemies)\b",
    "input": r"\b(?:curItem|curUser|curAction|selectedClass|curSlot)\b",
    "global progress": r"\b(?:Statistics|Notes|Badges|Journal|GamesInProgress)\.",
}
print("group\tpath\tlines (one entry per match)")
files = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", revision]).decode().splitlines()
for path in files:
    if not path.endswith(".java"):
        continue
    source = subprocess.check_output(["git", "show", f"{revision}:{path}"]).decode()
    for group, pattern in groups.items():
        lines = [str(source.count("\n", 0, match.start()) + 1) for match in re.finditer(pattern, source)]
        if lines:
            print(group, path, ",".join(lines), sep="\t")
