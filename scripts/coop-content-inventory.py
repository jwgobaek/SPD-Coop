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
    "items/wands/WandOfFireblast": "native cone, all three charge bands, burning, Cripple and Paralysis, cold restore and lost reply on each seat / CoopMageTest, HostRestartTest",
    "items/wands/WandOfBlastWave": "identified uncursed handheld, both owners, native hit and displacement, lost reply and reauthentication / HostRestartTest",
    "actors/mobs/Necromancer": "native summon, occupied arrival, heal, Adrenaline and re-summon, both seats, disk crash and replay / NecromancerTest, HostRestartTest",
    "actors/mobs/Necromancer.NecroSkeleton": "native minion ID, parent death, freeze at floor 8 and restore / NecromancerTest, LatePrisonTest",
    "actors/mobs/SpectralNecromancer": "isolated native wraith summons, identity on restore and caster death cleanup / NecromancerTest",
    "actors/mobs/Bat": "native prison 9 generation and life-steal proc against both seats / LatePrisonTest",
    "actors/mobs/DM100": "native aimed bolt hits each seat separately, cold restore and next turn / PrisonCombatChecks, HostRestartTest",
    "actors/mobs/Guard": "native chain pull, landing, cripple, spent-chain restore on either seat / PrisonCombatChecks, HostRestartTest",
    "actors/mobs/Skeleton": "floor six generation, native melee and death burst hitting adjacent heroes / SewerDepthTest, HostRestartTest",
    "actors/mobs/npcs/Shopkeeper": "floor six native stock, price, sale and seller-bound buyback on both seats after process death / HostRestartTest; special merchandise and theft remain gated",
    "items/weapon/missiles/ThrowingKnife": "plain unmodified native projectile, both seats, lodging and durable receipt after host death / PrisonProjectileTest, HostRestartTest",
    "actors/buffs/PinCushion": "two owners, native death/drop, dormant and active physical IDs / PrisonProjectileTest",
    "actors/mobs/ArmoredStatue": "716 Mail/Potential, 446 Leather/Repulsion, 571 Leather/AntiMagic and 578 Leather/Swiftness; two seats, parked equipment IDs, loot and confirmed strike after restart / SewerGuardiansTest, HostRestartTest",
    "items/armor/glyphs/Potential": "armored statue defends; native non-Hero wearer does not charge the attacking mage's wand / SewerGuardiansTest",
    "items/armor/glyphs/Repulsion": "native statue proc queues one push with landing on the authoritative scheduler / SewerGuardiansTest",
    "items/scrolls/ScrollOfIdentify": "known scroll, privately select owned supported equipment, identify once, reconnect / HostRestartTest",
    "items/scrolls/ScrollOfUpgrade": "known scroll, upgrade owned supported equipment; committed use survives response loss and cold restart / HostRestartTest",
    "items/weapon/missiles/ThrowingStone": "plain unupgraded stone, native hit, wear, break, pickup, own-only stack and shortcut / CoopPlayInputTest, HostRestartTest",
    "levels/PrisonLevel": "original 6 from normal start, 7 by two-seat staircase fixture, lost response, cold restart and return / HostRestartTest",
    "actors/mobs/Mimic": "seed 408 native disguise, opener, melee, missile, gold, parked floor, receipt / SewerMimicTest, HostRestartTest",
    "actors/mobs/CrystalMimic": "seed 411 ambush, victim stack, escape or return on defeat, two process kills / SewerCrystalMimicTest, HostRestartTest",
    "actors/mobs/Statue": "seeds 416/420/421/428/434/435/437/444; passive base statistics, native on-hit effect, held identity and drop / SewerGuardiansTest, HostRestartTest",
    "items/weapon/enchantments/Venomous": "natural seed 444 statue, both seats, poison ticks and identity after restore / SewerGuardiansTest",
    "items/weapon/enchantments/Chilling": "isolated statue proc applies chill to struck hero on either seat / SewerGuardiansTest",
    "items/weapon/enchantments/Shocking": "isolated statue chain, nearby other seat takes damage once / SewerGuardiansTest",
    "actors/mobs/GnollExile": "seed 420 passive, attacked by both seats, native drops independent of Ghost quest / SewerGuardiansTest",
    "actors/mobs/PhantomPiranha": "seed 479 distant missile from each seat, native water jump, return, melee defeat and meat drop / SewerGuardiansTest",
    "items/weapon/enchantments/Kinetic": "seed 416 statue's own Sickle, tracker on wielder, original damage handling / SewerGuardiansTest",
    "items/weapon/enchantments/Vampiric": "seed 420 statue's own BattleAxe, native heal on hit / SewerGuardiansTest",
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
    "items/wands/WandOfMagicMissile": "starting staff and identified uncursed standalone Magic Missile on either seat, charges, owner, quickslot and crash receipt / CoopMageTest, HostRestartTest",
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
VERIFIED.update({
    "actors/mobs/GoldenMimic": "427/428 disguised locked chest, native ambush and prize, durable receipt / SewerMimicTest, HostRestartTest",
    "items/weapon/enchantments/Unstable": "421 native uniform draw of 14 delegates on a statue, both defending seats / UnstableSewerTest",
    "items/weapon/enchantments/Projecting": "435 statue melee reach / SewerGuardiansTest",
    "items/keys/IronKey": "native pickup, depth journal shared by both seats, durable door consumption / SewerHazardsTest, HostRestartTest",
    "levels/Terrain.LOCKED_DOOR": "spend an iron key and enter the opened door, restore after host death / SewerHazardsTest, HostRestartTest",
    "levels/traps/WornDartTrap": "402 original visible trap, voluntary and forced arrival, nearest target and deferred damage / SewerHazardsTest",
    "actors/mobs/npcs/Sheep": "FlockTrap spawn, actor identity across restore and native expiry / SewerHazardsTest",
})
for effect in ("Blazing","Blocking","Blooming","Corrupting","Elastic","Grim","Lucky","Vorpal","Eldritch"):
    VERIFIED["items/weapon/enchantments/"+effect] = "Unstable statue attacking a hero; see per-effect limitations in unstable-effects.md / UnstableSewerTest"
for trap in ("BurningTrap","ChillingTrap","ShockingTrap","ToxicTrap","ConfusionTrap","OozeTrap",
             "AlarmTrap","FlockTrap","SummoningTrap","TeleportationTrap"):
    VERIFIED["levels/traps/"+trap] = "exact native trap, either seat, active-floor checkpoint and subsequent action / SewerHazardsTest"
VERIFIED.update({
    "actors/mobs/npcs/Ghost": "depths 2–4 original shared quest, separate native opponents, one owned selection / SewerGhostTest",
    "actors/mobs/GnollTrickster": "naturally offered depth 3 quest, native combat, defeat, restore and reward / SewerGhostTest",
    "actors/mobs/GreatCrab": "naturally offered depth 4 quest, original block, combat, restore and reward / SewerGhostTest",
    "actors/mobs/Goo": "original charged selected-target attack, water heal, seal, defeat, original drops and durable lost response / SewerDepthTest, HostRestartTest",
    "actors/mobs/Thief": "steal one unit from either victim, flee, preserve physical identity and owner on parked floor / SewerResidentsTest",
    "actors/mobs/Swarm": "native split, health division and distinct scheduled actor ID / SewerResidentsTest",
    "actors/mobs/CausticSlime": "native ooze applied to the struck seat / SewerResidentsTest",
    "actors/buffs/Healing": "native healing ticks, remaining buff and HP cap / SewerTravelGearTest",
    "items/potions/PotionOfHealing": "identified owned draught on each living seat, cure, native healing, receipt / SewerTravelGearTest, HostRestartTest",
    "items/weapon/SpiritBow": "unenhanced huntress bow, visible ordinary enemy, native shot for both seats, reconnect / SewerTravelGearTest, HostRestartTest",
    "items/weapon/SpiritBow.SpiritArrow": "native targeting, owner-scaled damage, attack time and return to melee equipment / SewerTravelGearTest",
    "items/armor/ClothArmor": "explicitly remove sealed warrior armor, owner and seal preserved after restore / SewerTravelGearTest",
    "items/armor/LeatherArmor": "equip plain armor on both seats, retain equipment on restore / SewerTravelGearTest",
    "items/keys/WornKey": "Goo drops one, locked exit consumes one shared depth key / SewerDepthTest, HostRestartTest",
    "items/quest/GooBlob": "original 2–4 quantity across host termination after HEAD / SewerDepthTest, HostRestartTest",
    "levels/SewerLevel": "branch zero generated depths 1–4, parked actors and items, gate survey / SewerDepthTest",
    "levels/SewerBossLevel": "original fifth floor, both seat locks, dry combat fixture and reopened exit / SewerDepthTest",
    "levels/Terrain.LOCKED_EXIT": "original boss exit, depth key consumption / SewerDepthTest",
    "levels/Terrain.UNLOCKED_EXIT": "unlocked exit persists across checkpoint, depth 6 excluded / SewerDepthTest",
    "actors/mobs/HermitCrab": "original armor drop uses credited seat level, survives checkpoint / SewerResidentsTest",
    "plants/Earthroot": "native Armor buff on the occupant of either seat, persists on restore / SewerTravelGearTest",
    "plants/Sorrowmoss": "native Poison on either occupant, restored with remaining effect / SewerTravelGearTest",
    "plants/Starflower": "native Bless buff on the occupant of either seat, persists on restore / SewerTravelGearTest",
    "plants/Fadeleaf": "original same-floor teleport, both basic hero seats, withering persists / SewerTravelGearTest",
})
VERIFIED.update({
    "plants/Blindweed": "landing blindness and cripple / SewerPlantTest, HostRestartTest",
    "plants/Stormvine": "landing vertigo / SewerPlantTest, HostRestartTest",
    "plants/Mageroyal": "cleansing the occupant / SewerPlantTest, HostRestartTest",
    "plants/Sungrass": "stationary healing at the landing / SewerPlantTest, HostRestartTest",
    "plants/Sungrass.Health": "partial stationary healing on the original cell, expiry after leaving / SewerPlantTest, HostRestartTest",
    "plants/Swiftthistle": "time bubble on the occupant / SewerPlantTest, HostRestartTest",
    "plants/Swiftthistle.TimeBubble": "private delayed terrain press, expiry and resumed turn / SewerPlantTest, HostRestartTest",
    "plants/Firebloom": "native fire blob, occupant burning and withering / SewerPlantTest, HostRestartTest",
    "plants/Icecap": "native frost, one-shot carried shatter, adjacent floor splash / SewerPlantTest, HostRestartTest",
})
for trap in ("GrippingTrap","RockfallTrap","GatewayTrap"):
    VERIFIED["levels/traps/"+trap]="native effect on either seat, same-floor checkpoint, original gateway destination / SewerTravelGearTest"
VERIFIED.update({
    "actors/mobs/npcs/Wandmaker": "adjacent accept and owned turn-in, two native candidates, both seats / WandmakerTest, HostRestartTest",
    "items/quest/CorpseDust": "haunted bones, pickup and return for either bearer / WandmakerTest",
    "items/quest/CorpseDust.DustGhostSpawner": "bearer buff, native summoning and active-floor removal / WandmakerTest",
    "items/quest/CorpseDust.DustWraith": "scheduled spawn and active-floor cleanup on turn-in / WandmakerTest",
    "items/quest/CeremonialCandle": "four native ritual cells with alternating owners / WandmakerTest",
    "actors/mobs/Elemental.NewbornFireElemental": "ritual summon, original combat and Embers drop / WandmakerTest",
    "levels/traps/GeyserTrap": "native water field, random center push, walked and forced landing on both seats, disk receipt / SewerHazardsTest, HostRestartTest",
    "levels/traps/PoisonDartTrap": "native VFX priority actor, poison, movement, knockback and guest crash receipt / SewerHazardsTest, HostRestartTest",
    "levels/rooms/special/ToxicGasRoom.ToxicVent": "original permanently disarmed vent in generated floor 6; persistent emitter after 6-7-6 / SewerHazardsTest",
    "levels/rooms/special/ToxicGasRoom.ToxicGasSeed": "parked emitter ID resumes native gas production / SewerHazardsTest",
    "items/quest/Embers": "owner pickup and one-time reward exchange / WandmakerTest, HostRestartTest",
    "actors/mobs/RotHeart": "native garden, battle, lasher cleanup and quest seed / WandmakerTest",
    "plants/Rotberry.Seed": "native quest drop and turn-in by either seat / WandmakerTest",
})
VERIFIED.update({
    "actors/mobs/Tengu": "native staged two-seat combat, teleports, two fights, victory, published receipts / TenguArenaTest, HostRestartTest",
    "actors/mobs/Tengu.BombAbility": "native throw, radius and timer, active-device restore and deterministic continuation / TenguArenaTest",
    "actors/mobs/Tengu.ShockerAbility": "native device, alternating cells, timer and original Actor ID / TenguArenaTest, HostRestartTest",
    "actors/mobs/Tengu.FireAbility": "native directed wave and burning, restore remaining cells and subsequent turns / TenguArenaTest",
    "items/TengusMask": "one original drop, first lawful pickup, ownership and receipt after restart; subclass use excluded / HostRestartTest",
    "levels/PrisonBossLevel": "five phases, off-map boss, stored items, seal, two heroes and WON revisit / TenguArenaTest, HostRestartTest",
    "levels/traps/TenguDartTrap": "native hidden dart activation during staged first fight / TenguArenaTest",
    "levels/CavesLevel": "floor 11 only, shop and native residents, two-seat arrival, restart and return / CavesEntranceTest, HostRestartTest",
    "actors/mobs/Brute": "floor 11, native rage at zero HP, restored actor, shield expiry and death / CavesEntranceTest",
    "actors/mobs/Brute.BruteRage": "original shield and expiration across a save / CavesEntranceTest",
    "items/weapon/melee/RunicBlade": "seed 572 cave armored statue only, passive damage, chain proc, mail and blade IDs and drops; duelist ability excluded / CavesEntranceTest",
})
for shaman in ("RedShaman", "BlueShaman", "PurpleShaman"):
    VERIFIED["actors/mobs/Shaman."+shaman] = "native earthen bolt and specific debuff against each seat / CavesEntranceTest"
IMPLEMENTED = {
    "actors/mobs/RotLasher": "generated garden resident, native cleanup with RotHeart; isolated combat pending",
    "items/armor/MailArmor": "plain unglyphed armor ownership and equipment path, separate item-specific trial pending",
    "actors/mobs/Crab": "native basic AI in generated upper sewers, isolated per-seat case pending",
    "actors/mobs/Slime": "native basic AI in generated upper sewers, isolated per-seat case pending",
    "actors/mobs/npcs/RatKing": "resident on the fifth floor, audience and crown quest unavailable",
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

