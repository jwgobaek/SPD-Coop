#!/usr/bin/env python3
"""Index player-facing v4.0.2 sources and the scope of item evidence in the coop ledger."""
import csv
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
REVISION = "57a4e06a4caf162446d1c28caa7983f0493fecf0"
GROUPS = [
    ("tipped darts", "items/weapon/missiles/darts/"), ("missiles", "items/weapon/missiles/"),
    ("melee weapons and abilities", "items/weapon/melee/"), ("enchantments", "items/weapon/enchantments/"),
    ("armor glyphs", "items/armor/glyphs/"), ("armor", "items/armor/"),
    ("rings", "items/rings/"), ("artifacts", "items/artifacts/"), ("wands", "items/wands/"),
    ("potions", "items/potions/"), ("scrolls", "items/scrolls/"), ("seeds and plants", "plants/"),
    ("bombs", "items/bombs/"), ("alchemy spells", "items/spells/"), ("rare rooms", "levels/rooms/secret/"),
    ("special rooms", "levels/rooms/special/"), ("quest rooms", "levels/rooms/quest/"),
    ("quest items", "items/quest/"), ("quest NPCs", "actors/mobs/npcs/"),
    ("stones", "items/stones/"), ("food", "items/food/"),
]
SPECIAL = {"actors/hero/abilities/Ratmogrify":"Ratmogrify","Challenges":"challenge modes",
           "items/Recipe":"alchemy","items/weapon/SpiritBow":"spirit bow","items/AlchemyScroll":"alchemy",
           "items/food/Blandfruit":"food","actors/blobs/Alchemy":"alchemy"}


# Columns describe only the evidenced use. A reference selection, a starter
# item and a map drop are different observations.
STAGES = ("pickup", "desktop_ui", "native_use", "ownership", "restore", "tcp", "android")
EVIDENCE = {
    "items/artifacts/HolyTome": dict(pickup="STARTER", native_use="TESTED_SPELL_SUBSET",
        ownership="BOTH_SEATS", restore="YES", tcp="YES", android="SYNC37_TEN_SELECTORS"),
    "items/artifacts/CloakOfShadows": dict(pickup="STARTER", native_use="STEALTH_AND_SURPRISE",
        ownership="BOTH_SEATS", restore="YES", android="UPPER_GROWTH_EQUIPPED"),
    "items/artifacts/HornOfPlenty": dict(pickup="YES", native_use="CHARGED_MEAL", restore="YES"),
    "items/weapon/melee/Rapier": dict(pickup="STARTER", native_use="LUNGE", ownership="BOTH_SEATS",
        restore="YES", tcp="YES", android="WAND_ARTS_LUNGE"),
    "items/weapon/melee/Crossbow": dict(native_use="DART_AND_CHARGED_SHOT", ownership="BOTH_SEATS", restore="YES"),
    "items/weapon/melee/Quarterstaff": dict(native_use="DEFENSIVE_STANCE", ownership="BOTH_SEATS", restore="YES", tcp="YES"),
    "items/weapon/melee/RoundShield": dict(native_use="GUARD", ownership="BOTH_SEATS", restore="YES", tcp="YES"),
    "items/weapon/melee/Dirk": dict(pickup="NORMAL_VAULT", native_use="EQUIP", ownership="BOTH_SEATS", restore="YES"),
    "items/weapon/melee/Sickle": dict(pickup="NORMAL_VAULT", native_use="EQUIP", ownership="BOTH_SEATS", restore="YES"),
    "items/weapon/SpiritBow": dict(pickup="STARTER", native_use="ELEVEN_DIRECT_AND_SHARED",
        ownership="BOTH_SEATS", restore="YES", tcp="STONE_AND_SHARED_THROW", android="SYNC36_STONE_ENCHANT"),
    "items/weapon/missiles/darts/PoisonDart": dict(native_use="POISON_AND_WEAR", ownership="BOTH_SEATS", restore="YES", android="SYNC35_THROW"),
    "items/weapon/missiles/darts/BlindingDart": dict(native_use="BLINDNESS_AND_WEAR", ownership="BOTH_SEATS", restore="YES"),
    "items/scrolls/ScrollOfRecharging": dict(native_use="READ_AND_RECALL", restore="YES", tcp="YES", android="SYNC35_READ_AND_RECALL"),
    "items/scrolls/ScrollOfIdentify": dict(native_use="IDENTIFY_OWN_ITEM", restore="YES", tcp="YES", android="SYNC37_RECALL"),
    "items/scrolls/ScrollOfUpgrade": dict(native_use="UPGRADE_OWN_ITEM", restore="YES", tcp="YES"),
    "items/scrolls/exotic/ScrollOfEnchantment": dict(pickup="STAGED", desktop_ui="KO_EN", native_use="WORN_SUBSET",
        ownership="BOTH_SEATS", restore="YES", tcp="YES", android="SYNC37_FAIL"),
    "items/wands/WandOfPrismaticLight": dict(pickup="STAGED", desktop_ui="NOT_RUN", native_use="FIVE_PATHS",
        ownership="BOTH_SEATS", restore="YES", tcp="FIVE_POST_HEAD_BOUNDARIES", android="NOT_RUN"),
}
for wand in ("MagicMissile", "Fireblast", "BlastWave", "Frost", "Lightning", "Disintegration"):
    EVIDENCE["items/wands/WandOf" + wand] = dict(native_use="FIVE_PATHS", ownership="BOTH_SEATS", restore="YES")
EVIDENCE["items/wands/WandOfFrost"].update(desktop_ui="AIM_AND_FIRE", tcp="YES", android="SYNC36_TRINITY_CAST")
for enchantment in ("Vampiric", "Shocking", "Vorpal", "Eldritch", "Venomous"):
    EVIDENCE["items/weapon/enchantments/" + enchantment] = dict(pickup="NOT_APPLICABLE", native_use="BOW_AND_SHARED",
        ownership="BOTH_SEATS", restore="CHECKPOINT_WRITTEN", desktop_ui="NOT_RUN", tcp="NOT_RUN", android="NOT_RUN")


def main():
    files = subprocess.check_output(["git","ls-tree","-r","--name-only",REVISION,"--",BASE],cwd=ROOT,text=True)
    generator=subprocess.check_output(["git","show",REVISION+":"+BASE+"items/Generator.java"],cwd=ROOT,text=True)
    refs=set(re.findall(r"\b([A-Za-z0-9_]+)\.class",generator))
    with (ROOT/"docs/coop/content-v4.0.2.tsv").open() as src:
        ledger={r["declaration"]:r for r in csv.DictReader(src,delimiter="\t")}
    writer=csv.writer(sys.stdout,delimiter="\t",lineterminator="\n")
    writer.writerow(("category","v402_source","generator_class_reference","legacy_scope","pickup","desktop_ui",
                     "native_use","ownership","restore","tcp","android","scope_and_remaining"))
    for path in files.splitlines():
        if not path.endswith(".java"): continue
        key=path[len(BASE):-5]
        group=SPECIAL.get(key) or next((g for g,prefix in GROUPS if key.startswith(prefix)),None)
        if group is None: continue
        row=ledger.get(key,{})
        stages = [EVIDENCE.get(key, {}).get(stage, "UNASSESSED") for stage in STAGES]
        note = row.get("scope_or_evidence") or "NO_INDEXED_EVIDENCE"
        writer.writerow((group,key,key.rsplit("/",1)[-1] in refs,row.get("coop_status","UNASSESSED"),*stages,note))


if __name__ == "__main__":
    main()
