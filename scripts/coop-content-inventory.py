#!/usr/bin/env python3
"""Inventory original declarations from the pinned tag. Status applies only to the named operation."""
import csv
import io
import re
import subprocess
import sys
import tarfile

BASE = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
TAG = "v4.0.2"
PREFIXES = ("items/", "actors/mobs/", "actors/buffs/", "actors/blobs/", "actors/hero/abilities/",
            "actors/hero/spells/", "levels/", "plants/", "mechanics/")
ENUMS = {"HeroClass", "HeroSubClass", "Talent"}
VERIFIED = {
    "actors/mobs/npcs/Ghost": "depth 2 shared quest, single owned reward, reboot and receipt / SewerGhostTest, HostRestartTest",
    "actors/mobs/FetidRat": "spawned quest, actual combat, credited completion / SewerGhostTest, HostRestartTest",
    "actors/mobs/Bee": "host and guest, guarding a pot or holder, parked and active targets / SewerBeeTest",
    "items/Honeypot": "owned pickup and shatter on self, real summoned bee / SewerBeeTest, HostRestartTest",
    "items/Honeypot.ShatteredPot": "bee ID, owner, drop and return from a parked floor / SewerBeeTest",
    "levels/rooms/secret/SecretHoneypotRoom": "original generated bee and broken pot (bomb use excluded) / SewerGenerationTest, SewerBeeTest",
    "actors/mobs/Piranha": "native water AI, seat combat, loot, parked floor / SewerSpecialActorTest",
    "levels/rooms/special/SentryRoom.Sentry": "native charge, one eligible seat, damage and save boundary / SewerSpecialActorTest",
    "actors/hero/HeroClass.MAGE": "starting staff, two seats, disk restart / CoopMageTest, HostRestartTest",
    "items/weapon/melee/MagesStaff": "starting equipment, equip, unequip, charge, cast and restore / CoopMageTest",
    "items/wands/WandOfMagicMissile": "non-cursed starting staff missile against a visible basic enemy / CoopMageTest, HostRestartTest",
    "items/wands/Wand.Charger": "staff charge recovery, owner, actor ID and partial charge restore / CoopMageTest",
    "actors/buffs/WandEmpower": "damage and charge effects belong to the caster / CoopMageTest",
    "actors/mobs/Rat": "adjacent attack, lethal hit and revival setup / CoopPlayInputTest",
    "items/food/Food": "pickup, split, drop, eat with two seats / CoopPlayInputTest, RuleIntegrationTest",
    "items/food/SmallRation": "two seats, hunger, quantity, ownership, restore and real TCP reconnect / CoopContentPathTest, CoopPlayInputTest",
    "levels/Terrain.DOOR": "ordinary door: enter, occupancy, save, restore and reconnect / CoopContentPathTest, CoopPlayInputTest",
    "levels/Terrain.OPEN_DOOR": "ordinary door: leave and close with original action cost / CoopContentPathTest",
    "actors/buffs/Hunger": "ordinary and small rations; seat-specific satiety and cost / CoopContentPathTest",
    "actors/buffs/Foresight": "one seat maps terrain without changing the other seat / ProjectionChecks",
    "actors/buffs/Poison": "lethal damage and pre-death HP from a damage tick / CoopSessionTest",
    "levels/SewerLevel": "native floors 1 and 2, walk, carry two heroes, park and revisit / CoopFloorsTest, HostRestartTest",
    "levels/Terrain.ENTRANCE": "sewer 2 to 1, two-seat consent, fresh-JVM resume / HostRestartTest",
    "levels/Terrain.EXIT": "sewer 1 to 2, native walking route, duplicate receipt / CoopFloorsTest, HostRestartTest",
    "levels/Terrain.EMPTY": "ordinary adjacent movement, seat occupancy / CoopSessionTest",
    "levels/Terrain.WALL": "blocked movement, independent visibility and zone separation / CoopSessionTest",
    "levels/Terrain.HIGH_GRASS": "original huntress trample on both seats / SewerBeeTest",
    "levels/Terrain.FURROWED_GRASS": "disk-world restore and warrior trample on both seats / SewerBeeTest",
    "levels/Terrain.GRASS": "remember, hide an unseen change and reveal it when observed / ProjectionChecks",
    "levels/Terrain.SECRET_DOOR": "hidden appearance, adjacent intentional host search, native route / ProjectionChecks, HostRestartTest",
    "levels/Terrain.SECRET_TRAP": "hide undiscovered terrain identity from recipient / ProjectionChecks",
    "items/Ankh": "owned revival resource, save, consume once / CheckpointTest, ReconnectTest",
    "items/weapon/melee/Gloves": "starter equip and unequip / CoopPlayInputTest",
    "items/weapon/melee/Shortsword": "pickup and equip / SyncIntegrationTest",
    "actors/buffs/Haste": "seat and scheduler round trip / CheckpointTest",
    "actors/hero/HeroClass.WARRIOR": "first-floor basic actions / CoopControllerTest, CoopPlayInputTest",
    "actors/hero/HeroClass.HUNTRESS": "first-floor basic actions / CoopControllerTest, CoopPlayInputTest",
}
IMPLEMENTED = {
    **dict.fromkeys(("levels/Terrain.EMPTY_SP", "levels/Terrain.WATER", "levels/Terrain.EMBERS",
                     "levels/Terrain.EMPTY_DECO"), "safeFloor movement allowlist for unoccupied plain cells"),
    **dict.fromkeys(("actors/mobs/Albino", "actors/mobs/Gnoll", "actors/mobs/Snake"), "basicMob allowlist"),
    **dict.fromkeys(("items/weapon/melee/Dagger", "items/weapon/melee/WornShortsword"), "basicWeapon allowlist"),
}


def main():
    archive = subprocess.check_output(["git", "archive", TAG, BASE])
    rows = []
    with tarfile.open(fileobj=io.BytesIO(archive)) as tree:
        for member in tree:
            if not member.isfile() or not member.name.endswith(".java"):
                continue
            path = member.name[len(BASE):-5]
            name = path.rsplit("/", 1)[-1]
            src = tree.extractfile(member).read().decode()
            src = re.sub(r"/\*.*?\*/|//[^\n]*", "", src, flags=re.S)
            if name in ENUMS and path.startswith("actors/hero/"):
                options = src.split("enum " + name, 1)[1].split("{", 1)[1].split(";", 1)[0]
                for option in re.findall(r"\b([A-Z][A-Z_0-9]+)\s*\(", options):
                    if option != "NONE":
                        rows.append((path+"."+option, name, member.name))
            if path == "levels/Terrain":
                for tile in re.findall(r"static final int\s+(\w+)\s*=", src.split("int PASSABLE",1)[0]):
                    rows.append((path+"."+tile, "terrain", member.name))
            if path.startswith(PREFIXES):
                if path.startswith("actors/mobs/npcs/"):
                    group = "NPC"
                elif path.startswith("items/weapon/") and path.count("/") >= 3:
                    group = "/".join(path.split("/")[:3])
                else:
                    group = path.rsplit("/",1)[0]
                rows.append((path, group, member.name))
                # Named nested effects, rewards and summons are tracked with their enclosing source.
                for inner in set(re.findall(r"\bclass\s+(\w+)\s+(?:extends|implements)\b", src)) - {name}:
                    rows.append((path+"."+inner, group, member.name))
    writer = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
    writer.writerow(("declaration", "category", "coop_status", "scope_or_evidence", "upstream_source"))
    for key, category, source in sorted(set(rows)):
        status = "VERIFIED" if key in VERIFIED else "IMPLEMENTED" if key in IMPLEMENTED else "UNSUPPORTED"
        writer.writerow((key, category, status, VERIFIED.get(key, IMPLEMENTED.get(key, "")), source))


if __name__ == "__main__":
    main()

