"""Triage the upstream Dungeon.hero inventory. Numbers refer to the upstream tag."""
from collections import defaultdict
from pathlib import Path
import re
import subprocess

REVISION = "57a4e06a4caf162446d1c28caa7983f0493fecf0"
HERO = re.compile(r"\bDungeon\s*\.\s*hero\b")
INVENTORY = Path("docs/coop/dependencies.tsv")


def role(path, before, after):
    """Primary reading for this occurrence. Semantic review is still needed at call sites."""
    name = Path(path).stem
    if name in {"Rankings", "GamesInProgress", "WndRanking", "Bones"}:
        return "solo"
    if name in {"Badges", "Statistics", "Journal", "Notes"}:
        return "world"
    if re.match(r"\s*\.\s*(fieldOfView|viewDistance|mindVisionEnemies|visibleEnemies)", after):
        return "observer"
    if re.match(r"\s*\.\s*belongings", after):
        return "owner"
    if re.match(r"\s*\.\s*(damage|heal|die|HP\b|HT\b)", after):
        return "target"
    if re.search(r"(Buff\.(affect|append|prolong|detach)|attack|aggro|damage|isTargeting)\(\s*$", before):
        return "target"
    if re.search(r"(isEquipped|activate|doEquip|collect|detachAll)\(\s*$", before):
        return "owner"
    if "/windows/" in path or "/ui/" in path or "/scenes/" in path:
        return "local-ui"
    if re.search(r"(fieldOfView\[|observe\(\s*)$", before):
        return "observer"
    if re.search(r"(enemy|target|defender)\s*[!=]=?\s*$", before):
        return "target"
    if name == "Dungeon" and re.match(r"\s*=\s*[^=]", after):
        return "world"
    if re.match(r"\s*\.\s*(act|ready|handle|attack|spend|busy|next|interrupt|rest)\b", after):
        return "actor"
    if "/items/" in path:
        return "owner?"
    if "/actors/mobs/" in path or "/traps/" in path:
        return "target?"
    if "/sprites/" in path or "/effects/" in path:
        return "local-ui?"
    return "actor?"


def main():
    print("upstream Java path\tprimary role\tupstream line per occurrence")
    for entry in INVENTORY.read_text().splitlines()[1:]:
        group, path, locations = entry.split("\t")
        if group != "hero":
            continue
        source = subprocess.check_output(["git", "show", f"{REVISION}:{path}"], text=True)
        roles = defaultdict(list)
        lines = []
        for match in HERO.finditer(source):
            line = source.count("\n", 0, match.start()) + 1
            before = source[source.rfind("\n", 0, match.start()) + 1:match.start()]
            after = source[match.end():source.find("\n", match.end())]
            roles[role(path, before, after)].append(str(line))
            lines.append(str(line))
        if ",".join(lines) != locations:
            raise ValueError(f"Phase 1 inventory differs at {path}")
        for label, sites in sorted(roles.items()):
            print(path, label, ",".join(sites), sep="\t")


if __name__ == "__main__":
    main()
