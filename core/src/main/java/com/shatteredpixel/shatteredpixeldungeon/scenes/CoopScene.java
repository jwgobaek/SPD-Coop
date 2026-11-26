package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.TrinityDiscoveries;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndCombo;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseSubclass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatInventory;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatTrade;
import com.watabou.noosa.*;
import java.util.UUID;

/** All widgets are render-thread only. Scene reconstruction keeps the live controller. */
public class CoopScene extends PixelScene {
    private String lastVaultWarning="";
    private static CoopController active;
    private CoopController controller;
    private CoopController.Frame shown;
    private Group panel;
    private CoopBoard board;
    private CoopHud hud;
    private int first;
    private float left,top,width,bottom,center;
    private boolean exiting;
    private long effectsVersion;
    private String targetMove;
    private PublicDungeon targetView;
    private PublicDungeon.Loot targetStaff;

    public void useItem(PublicDungeon view, PublicDungeon.Loot item, String action) {
        if (controller.frame().image!=view.source || !view.actions(item).contains(action)) return;
        switch (action) {
            case "CAST":
                showSpells(view,item);
                break;
            case "WEAPON_ABILITY":
                if (item.sprite==ItemSpriteSheet.RAPIER) {
                    targetView=view; targetMove=action; targetStaff=item; renderFrame();
                } else controller.act(view.source,action,view.pos,item.id);
                break;
            case "AFFIX_SEAL":
                selectSealArmor(view,item);
                break;
            case "IMBUE":
                chooseWand(view,item);
                break;
            case "USE_CROWN":
                crownWindow(view,item);
                break;
            case "ARMOR_ABILITY":
                String ability = view.source.rows.get("ability"+item.id);
                if ("Trinity".equals(ability)) {
                    java.util.ArrayList<Talent> forms=new java.util.ArrayList<>();
                    java.util.ArrayList<String> names=new java.util.ArrayList<>();
                    for (Talent form : new Talent[]{Talent.BODY_FORM,Talent.MIND_FORM,Talent.SPIRIT_FORM}) if (view.source.rows.containsKey("trinity"+form.ordinal())) {
                        forms.add(form); names.add(form.title());
                    }
                    names.add(msg("back"));
                    add(new WndOptions(Messages.get(Trinity.class,"name"),Messages.get(Trinity.WndUseTrinity.class,"text"),names.toArray(new String[0])) {
                        protected void onSelect(int index) {
                            if (index<0 || index>=forms.size() || controller.frame().image!=view.source) return;
                            if (forms.get(index)==Talent.MIND_FORM) { targetView=view;targetMove="TRINITY_MIND";targetStaff=item;renderFrame(); }
                            else controller.act(view.source,forms.get(index)==Talent.BODY_FORM ? action : "TRINITY_SPIRIT",view.pos,item.id);
                        }
                    });
                } else if ("WarpBeacon".equals(ability) && view.source.rows.containsKey("beacondepth")) {
                    Class<?> warp=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.class;
                    add(new WndOptions(Messages.get(warp,"name"), Messages.get(warp,"window_desc",Integer.parseInt(view.source.rows.get("beacondepth"))),
                            Messages.get(warp,"window_tele"),Messages.get(warp,"window_clear"),Messages.get(warp,"window_cancel")) {
                        @Override protected void onSelect(int index) {
                            if (index==0 || index==1) controller.act(view.source,action,index==0 ?
                                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.RECALL :
                                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.CLEAR,item.id);
                        }
                    });
                } else if (("SpiritHawk".equals(ability) && view.source.rows.containsKey("hawkcommand")) || ("ShadowClone".equals(ability) && view.source.rows.containsKey("clonecommand")) || "Challenge".equals(ability) || "ElementalStrike".equals(ability) || "Feint".equals(ability) || "PowerOfMany".equals(ability) || "DeathMark".equals(ability) || "SmokeBomb".equals(ability) || "WildMagic".equals(ability) || "HeroicLeap".equals(ability) || "Shockwave".equals(ability) || "SpectralBlades".equals(ability)) {
                    targetView=view; targetMove=action; targetStaff=item; renderFrame();
                } else controller.act(view.source,action,view.pos,item.id);
                break;
            case "USE_MASK":
                subclassWindow(view,item);
                break;
            case "ZAP": case "SHOOT": case "THROW":
                aim(view,item);
                break;
            case "SELL": case "SELL_ONE":
                WndSeatTrade.sell(this,view,item,action,controller);
                break;
            case "UPGRADE": case "IDENTIFY":
                add(WndSeatInventory.chooseEquipment(view,controller,item));
                break;
            default:
                view.use(controller,item,action);
        }
    }

    private void showSpells(PublicDungeon view, PublicDungeon.Loot tome) {
        java.util.ArrayList<ClericSpell> spells = new java.util.ArrayList<>();
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();
        spells.add(GuidingLight.INSTANCE); actions.add("GUIDING_LIGHT");
        spells.add(HolyWeapon.INSTANCE); actions.add("HOLY_WEAPON");
        spells.add(HolyWard.INSTANCE); actions.add("HOLY_WARD");
        for (Talent talent : new Talent[]{Talent.SHIELD_OF_LIGHT, Talent.SUNRAY, Talent.DIVINE_SENSE}) {
            if (!view.source.rows.containsKey("talent"+talent.ordinal())) continue;
            spells.add(talent == Talent.SHIELD_OF_LIGHT ? ShieldOfLight.INSTANCE :
                    talent == Talent.SUNRAY ? Sunray.INSTANCE : DivineSense.INSTANCE);
            actions.add(talent.name());
        }
        for (Talent talent : new Talent[]{Talent.HOLY_INTUITION,Talent.BLESS,Talent.RECALL_INSCRIPTION,Talent.BODY_FORM,Talent.MIND_FORM,Talent.SPIRIT_FORM}) {
            if (view.source.rows.containsKey("talent"+talent.ordinal())) {
                spells.add(talent==Talent.RECALL_INSCRIPTION ? RecallInscription.INSTANCE : talent==Talent.SPIRIT_FORM ? SpiritForm.INSTANCE : talent==Talent.MIND_FORM ? MindForm.INSTANCE : talent==Talent.BODY_FORM ? BodyForm.INSTANCE : talent==Talent.BLESS ? BlessSpell.INSTANCE : HolyIntuition.INSTANCE);
                actions.add(talent.name());
            }
        }
        String subclass = view.source.rows.get("subclass");
        if ("PRIEST".equals(subclass)) { spells.add(Radiance.INSTANCE); actions.add("RADIANCE"); }
        if ("PALADIN".equals(subclass)) { spells.add(Smite.INSTANCE); actions.add("SMITE"); }
        String[] choices=new String[spells.size()+1];
        for (int i=0;i<spells.size();i++) choices[i]=spells.get(i).name();
        choices[spells.size()]=msg("back");
        add(new WndOptions(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(tome.sprite),
                Messages.get("items.artifacts.holytome.name"),msg("charges",tome.charges,tome.capacity),choices) {
            protected void onSelect(int index) {
                if (index<0 || index>=spells.size() || controller.frame().image!=view.source) return;
                String action = actions.get(index);
                if (action.equals("HOLY_INTUITION") || action.equals("RECALL_INSCRIPTION") || action.endsWith("_FORM")) {
                    showSpellTargets(view, tome, action, spells.get(index));
                } else if (spells.get(index) instanceof TargetedClericSpell) { targetView=view; targetMove=action; targetStaff=tome; renderFrame(); }
                else controller.act(view.source,action,view.pos,tome.id);
            }
        });
    }

    private void showSpellTargets(PublicDungeon view, PublicDungeon.Loot tome, String action, ClericSpell spell) {
        java.util.ArrayList<TrinityDiscoveries.Choice> remembered = new java.util.ArrayList<>();
        if (action.endsWith("_FORM")) {
            for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values()) {
                if (choice.form.name().equals(action) && view.source.rows.containsKey(choice.rowKey()))
                    remembered.add(choice);
            }
        }
        if (remembered.isEmpty()) {
            showSpellInventory(view, tome, action);
            return;
        }
        String[] options = new String[remembered.size() + 1];
        options[0] = msg("bag");
        for (int i = 0; i < remembered.size(); i++) {
            options[i + 1] = Messages.titleCase(Messages.get(remembered.get(i).effect, "name", "").trim());
        }
        add(new WndOptions(spell.name(), Messages.get(Trinity.WndItemtypeSelect.class, "text"), options) {
            @Override protected void onSelect(int index) {
                if (index < 0 || index >= options.length || controller.frame().image != view.source) return;
                if (index == 0) showSpellInventory(view, tome, action);
                else controller.act(view.source, action, remembered.get(index - 1).target, tome.id);
            }
        });
    }

    private void showSpellInventory(PublicDungeon view, PublicDungeon.Loot tome, String action) {
        String prefix = action.equals("RECALL_INSCRIPTION") ? "recallitem" : action.equals("MIND_FORM") ? "minditem" :
                action.equals("SPIRIT_FORM") ? "spirititem" : action.equals("BODY_FORM") ? "bodyitem" : "intuition";
        add(WndSeatInventory.select(view, controller, new WndSeatInventory.Selection() {
            public boolean accepts(PublicDungeon.Loot candidate) { return view.source.rows.containsKey(prefix + candidate.id); }
            public void select(PublicDungeon.Loot candidate) {
                if (controller.frame().image == view.source && accepts(candidate))
                    controller.act(view.source, action, Integer.parseInt(view.source.rows.get(prefix + candidate.id)), tome.id);
            }
        }));
    }

    private void selectSealArmor(PublicDungeon view, PublicDungeon.Loot seal) {
        add(WndSeatInventory.select(view,controller,new WndSeatInventory.Selection() {
            public boolean accepts(PublicDungeon.Loot target) {
                return view.source.rows.containsKey("sealarmor"+target.id)
                        && (!view.source.rows.containsKey("sealcursetarget"+target.id) || view.source.rows.containsKey("sealcursed"+seal.id));
            }
            public void select(PublicDungeon.Loot target) {
                if (controller.frame().image!=view.source) return;
                int index=Integer.parseInt(view.source.rows.get("sealarmor"+target.id));
                String armor=view.source.rows.get("armorglyph"+target.id);
                String glyph=view.source.rows.get("affixseal"+seal.id);
                if (armor==null || glyph.equals("none") || armor.equals(glyph)) {
                    controller.act(view.source,"AFFIX_SEAL",index,seal.id);
                    return;
                }
                String a=Messages.get(armor+".name",new Object[]{""}).trim(), b=Messages.get(glyph+".name",new Object[]{""}).trim();
                Class<?> type=com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.class;
                add(new WndOptions(Messages.get(type,"choose_title"),Messages.get(type,"choose_desc",a,b),a,b,msg("cancel")) {
                    protected void onSelect(int choice) {
                        if ((choice==0 || choice==1) && controller.frame().image==view.source)
                            controller.act(view.source,choice==0 ? "KEEP_ARMOR_GLYPH" : "TRANSFER_SEAL_GLYPH",index,seal.id);
                    }
                });
            }
        }));
    }

    private void chooseWand(PublicDungeon view, PublicDungeon.Loot staff) {
        Class<?> type = com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff.class;
        add(WndSeatInventory.select(view,controller,new WndSeatInventory.Selection() {
            public boolean accepts(PublicDungeon.Loot item) {
                return view.source.rows.containsKey("wandchoice"+item.id);
            }
            public void select(PublicDungeon.Loot wand) {
                int selection = Integer.parseInt(view.source.rows.get("wandchoice"+wand.id));
                String description = Messages.get(type,"imbue_desc");
                if (view.source.rows.containsKey("imbuelevel"+staff.id)) {
                    int staffLevel = Integer.parseInt(view.source.rows.get("imbuelevel"+staff.id));
                    if (view.source.rows.containsKey("imbuelevel"+wand.id)) {
                        int wandLevel = Integer.parseInt(view.source.rows.get("imbuelevel"+wand.id));
                        int level = Math.max(staffLevel,wandLevel) + (staffLevel>0 && wandLevel>=staffLevel ? 1 : 0);
                        description += "\n\n"+Messages.get(type,"imbue_level",level);
                    } else description += "\n\n"+Messages.get(type,"imbue_unknown",staffLevel);
                }
                description += "\n\n"+Messages.get(type,"KEEP".equals(view.source.rows.get("imbue"+staff.id)) ? "imbue_talent" : "imbue_lost");
                add(new WndOptions(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(wand.sprite),
                        Messages.get(view.source.rows.get("label"+wand.id)+".name"),description,
                        Messages.get(type,"yes"),Messages.get(type,"no")) {
                    @Override protected void onSelect(int choice) {
                        if (choice==0 && controller.frame().image==view.source)
                            controller.act(view.source,"IMBUE",selection,staff.id);
                    }
                });
            }
        }));
    }

    public void aim(PublicDungeon view, PublicDungeon.Loot staff) {
        if (controller.frame().image!=view.source || (!view.actions(staff).contains("ZAP") && !view.actions(staff).contains("SHOOT") && !view.actions(staff).contains("THROW"))) return;
        targetView=view; targetStaff=staff; renderFrame();
    }
    void cancelAim() { targetView=null; targetStaff=null; targetMove=null; renderFrame(); }
    private void touchCell(PublicDungeon view,int cell) {
        if (hud!=null) hud.stopRest();
        if (targetView==null) {
            if(view.visible(cell) && (view.stair==cell || ((view.depth<5 || (view.depth>=6 && view.depth<com.shatteredpixel.shatteredpixeldungeon.CoopFloors.LAST_PLAYABLE)) && view.terrain(cell)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EXIT)
                    || (view.depth==5 && view.terrain(cell)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.UNLOCKED_EXIT)
                    || (view.depth>1 && view.terrain(cell)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.ENTRANCE))) {
                offerStairs(view,cell);
            } else if (view.visible(cell) && view.source.rows.containsKey("sale"+cell)) {
                WndSeatTrade.stock(this,view,cell,controller);
            } else if (view.source.rows.containsKey("merchant") && Integer.toString(cell).equals(view.source.rows.get("merchant"))) {
                WndSeatTrade.merchant(this,view,cell,controller);
            } else if ("VaultTokenDoor".equals(view.monsters.get(cell)) || "VaultMirror".equals(view.monsters.get(cell))) {
                String option=view.source.rows.get("vaultnpc"+cell);
                if (option==null) return;
                boolean mirror="VaultMirror".equals(view.monsters.get(cell));
                Class<?> npc=mirror ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror.class
                        : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor.class;
                if (view.offered && (option.equals("take") || option.equals("open"))) {
                    add(new WndOptions(Messages.titleCase(Messages.get(npc,"name")),Messages.get(npc,mirror?"approach":"enough_tokens"),
                            Messages.get(npc,option),msg("cancel")) {
                        @Override protected void onSelect(int index) {
                            if (index==0) controller.act(view.source,"VAULT_INTERACT",cell,0);
                        }
                    });
                } else alert(Messages.get(npc,option));
            } else if (Integer.toString(cell).equals(view.source.rows.get("ascentgate"))) {
                ascentWindow(shown);
            } else if ("Imp".equals(view.monsters.get(cell))) {
                Class<?> imp=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class;
                String offer=view.source.rows.get("imp"+cell);
                if("quest_intro_1".equals(offer) && view.offered) {
                    add(new WndOptions(Messages.get(imp,"name"),Messages.get(imp,offer),msg("accept"),msg("cancel")) {
                        @Override protected void onSelect(int index) {
                            if(index==0) controller.act(view.source,"ACCEPT_IMP",cell,0);
                        }
                    });
                } else if(offer!=null) alert(Messages.get(imp,offer));
            } else if ("Blacksmith".equals(view.monsters.get(cell))) {
                String offer=view.source.rows.get("smith"+cell);
                if (offer!=null) talkToSmith(view,cell,offer);
            } else if ("Wandmaker".equals(view.monsters.get(cell))) {
                String offer = view.source.rows.get("wandquest"+cell);
                if (offer != null) talkToWandmaker(view, cell, offer);
            } else if ("Ghost".equals(view.monsters.get(cell))) {
                String offer=view.source.rows.get("quest"+cell);
                if (offer!=null) talkToGhost(view,cell,offer);
            } else view.tap(controller,cell);
            return;
        }
        if (targetMove != null) {
            boolean armor = "TRINITY_MIND".equals(targetMove) || "BLESS".equals(targetMove) || "ARMOR_ABILITY".equals(targetMove) || "GUIDING_LIGHT".equals(targetMove) || "SHIELD_OF_LIGHT".equals(targetMove) || "SMITE".equals(targetMove) || "SUNRAY".equals(targetMove);
            boolean weapon = "WEAPON_ABILITY".equals(targetMove);
            if (targetView == view && (weapon || view.visible(cell) && (armor || "MONK_DASH".equals(targetMove) || view.monsters.containsKey(cell)))
                    && controller.act(view.source, targetMove, cell, armor || weapon ? targetStaff.id : 0)) cancelAim();
            return;
        }
        boolean bow=targetStaff.kind.equals("BOW");
        String actor=view.monsters.get(cell);
        boolean enemy=actor!=null && !"Imp".equals(actor) && !"Blacksmith".equals(actor) && !"Wandmaker".equals(actor) && !"Shopkeeper".equals(actor) && !"Sheep".equals(actor) && !"Sentry".equals(actor) && !"Ghost".equals(actor) && !"RatKing".equals(actor);
        boolean missile=PublicDungeon.isMissileKind(targetStaff.kind) || targetStaff.kind.equals("FOOD");
        boolean areaWand = view.source.rows.containsKey("areawand"+targetStaff.id) || (targetStaff.kind.equals("WAND") || targetStaff.kind.equals("UNIDENTIFIED_WAND")) && (targetStaff.sprite == ItemSpriteSheet.WAND_LIGHTNING || targetStaff.sprite == ItemSpriteSheet.WAND_FROST || targetStaff.sprite == ItemSpriteSheet.WAND_BLAST_WAVE
                || targetStaff.sprite == ItemSpriteSheet.WAND_FIREBOLT);
        boolean selfBattery = !missile && !bow && cell == view.pos && view.source.rows.containsKey("talent"+Talent.SHIELD_BATTERY.ordinal());
        if (targetView==view && (selfBattery || enemy || ((missile || areaWand) && actor==null && view.visible(cell)) || (!missile && !bow && view.chest(cell)))
                && controller.act(view.source,missile?"THROW":bow?"SHOOT":"ZAP",cell,targetStaff.id)) cancelAim();
    }
    void offerStairs(PublicDungeon view,int cell) {
        if (Integer.toString(cell).equals(view.source.rows.get("ascentgate"))) { ascentWindow(shown); return; }
        if(view.stair!=cell) alert(msg("stairconditions"));
        else if(view.offered && !view.consent) alert(msg("awaitvote"));
        else if (view.source.rows.containsKey("vaultscore") && (view.offered || !view.consent)) {
            int score=Integer.parseInt(view.source.rows.get("vaultscore"));
            Class<?> crystal=com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.class;
            add(new WndOptions(Messages.get(crystal,"name"),Messages.get(crystal,score<500?"leaving_early":score<=1000?"leaving_partly_explored":score<=2000?"leaving_fully_explored":score<4000?"leaving_partial_victory":"leaving_victory"),msg("accept"),msg("cancel")) {
                @Override protected void onSelect(int index) {
                    if (index!=0) return;
                    if (view.offered && score>=500) {
                        CoopScene.this.add(com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatInventory.select(view,controller,new com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatInventory.Selection() {
                            public boolean accepts(PublicDungeon.Loot item) { return view.source.rows.containsKey("vaultpick"+item.id); }
                            public void select(PublicDungeon.Loot item) { controller.act(view.source,"VAULT_REWARD",cell,item.id); }
                        }));
                    } else controller.act(view.source,view.offered?"TRAVEL":"CONSENT",cell,0);
                }
            });
        }
        else if (view.branch==1 && (view.offered || !view.consent)) {
            add(new WndOptions(msg("mine_exit"),msg("mine_final_exit"),msg("accept"),msg("cancel")) {
                @Override protected void onSelect(int index) {
                    if(index==0) controller.act(view.source,view.offered?"TRAVEL":"CONSENT",cell,0);
                }
            });
        } else controller.act(view.source,view.offered?"TRAVEL":view.consent?"CANCEL_TRAVEL":"CONSENT",cell,0);
    }
    private void talkToSmith(PublicDungeon view,int cell,String offer) {
        Class<?> smith=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.class;
        if (offer.equals("services")) { smithServices(view); return; }
        String body=Messages.get(smith,offer);
        if (offer.startsWith("reminder_") || !view.offered) { alert(body); return; }
        body=Messages.get(smith,"intro_quest_"+view.heroClass.toLowerCase(java.util.Locale.ROOT))+"\n\n"
                +Messages.get(smith,"intro_quest_start")+"\n\n"+body;
        add(new WndOptions(Messages.get(smith,"name"),body,msg("accept"),msg("cancel")) {
            @Override protected void onSelect(int choice) {
                if (choice==0) controller.act(view.source,"ACCEPT_BLACKSMITH",cell,0);
            }
        });
    }

    private void smithServices(PublicDungeon view) {
        java.util.List<String> labels=new java.util.ArrayList<>(), actions=new java.util.ArrayList<>();
        boolean choose=view.source.rows.containsKey("smithoffer0");
        if (choose) {
            for(int i=0;view.source.rows.containsKey("smithoffer"+i);i++) {
                String label=view.source.rows.get("smithoffer"+i);
                labels.add(Messages.get(label+".name"));
            }
        } else {
            for(String verb:new String[]{"PICK","REFORGE","HARDEN","UPGRADE","ORDER","CASH"}) {
                String cost=view.source.rows.get("price"+verb.toLowerCase(java.util.Locale.ROOT));
                if (cost!=null) { actions.add("SMITH_"+verb); labels.add(msg("service_"+verb.toLowerCase(java.util.Locale.ROOT),cost)); }
            }
        }
        int options=labels.size();
        labels.add(msg("cancel"));
        add(new WndOptions(Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.class,"name"),
                Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndBlacksmith.class,"prompt",Integer.parseInt(view.source.rows.get("favor"))),
                labels.toArray(new String[0])) {
            @Override protected void onSelect(int index) {
                if(!view.offered || index<0 || index>=options) return;
                if(choose) {controller.act(view.source,"SMITH_CLAIM",index,0);return;}
                String verb=actions.get(index);
                if(verb.equals("SMITH_REFORGE") || verb.equals("SMITH_HARDEN") || verb.equals("SMITH_UPGRADE"))
                    smithEquipment(view,verb,null);
                else controller.act(view.source,verb,0,0);
            }
        });
    }

    private void smithEquipment(PublicDungeon view,String verb,PublicDungeon.Loot first) {
        add(WndSeatInventory.select(view,controller,new WndSeatInventory.Selection() {
            public boolean accepts(PublicDungeon.Loot item) {
                String data=view.source.rows.get("forge"+item.id);
                if(data==null || first==item) return false;
                String[] fields=data.split(",");
                if(first!=null) return java.util.Objects.equals(view.source.rows.get("label"+first.id),view.source.rows.get("label"+item.id));
                return verb.equals("SMITH_REFORGE") || "1".equals(fields[verb.equals("SMITH_HARDEN")?1:2]);
            }
            public void select(PublicDungeon.Loot item) {
                if(first!=null) controller.act(view.source,verb,Integer.parseInt(view.source.rows.get("forge"+item.id).split(",")[0]),first.id);
                else if(verb.equals("SMITH_REFORGE")) smithEquipment(view,verb,item);
                else controller.act(view.source,verb,0,item.id);
            }
        }));
    }

    private void talkToWandmaker(PublicDungeon view, int cell, String offer) {
        String[] fields = offer.split(",");
        String kind = fields[0];
        if (fields[1].equals("HUNT")) {
            alert(Messages.get(Wandmaker.class, "reminder_"+kind,
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.valueOf(view.heroClass).title()));
            return;
        }
        if (!view.offered) return;
        boolean reward = fields[1].equals("REWARD");
        String body = reward ? Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndWandmaker.class, kind)
                : Messages.get(Wandmaker.class, "intro_"+kind) + Messages.get(Wandmaker.class, "intro_2");
        String[] options = reward ? new String[]{wandName(fields[2]), wandName(fields[3]), msg("cancel")}
                : new String[]{msg("accept"), msg("cancel")};
        add(new WndOptions(Messages.get(Wandmaker.class,"name"), body, options) {
            protected void onSelect(int choice) {
                if (choice >= 0 && choice < options.length-1)
                    controller.act(view.source, reward ? (choice == 0 ? "WAND_FIRST" : "WAND_SECOND") : "ACCEPT_WANDMAKER", cell, 0);
            }
        });
    }

    private String wandName(String type) {
        return Messages.titleCase(Messages.get("items.wands."+type.toLowerCase(java.util.Locale.ROOT)+".name"));
    }

    private void talkToGhost(PublicDungeon view,int cell,String offer) {
        Class<?> ghost=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.class;
        String title=Messages.get(ghost,"name");
        if (offer.equals("HUNT")) { alert(Messages.get(ghost,view.depth==3 ? "gnoll_2" : view.depth==4 ? "crab_2" : "rat_2")); return; }
        if (!view.offered) return;
        boolean reward=offer.equals("REWARD");
        String body=reward?Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndSadGhost.class,"give_item")
                :Messages.get(ghost,view.depth==3 ? "gnoll_1" : view.depth==4 ? "crab_1" : "rat_1",com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.valueOf(view.heroClass).title());
        String[] options=reward?new String[]{msg("ghost_weapon"),msg("ghost_armor"),msg("cancel")}
                :new String[]{msg("accept"),msg("cancel")};
        add(new WndOptions(title,body,options) {
            protected void onSelect(int choice) {
                if (choice>=0 && choice<options.length-1)
                    controller.act(view.source,reward?(choice==0?"GHOST_WEAPON":"GHOST_ARMOR"):"ACCEPT_GHOST",cell,0);
            }
        });
    }

    public static String msg(String key,Object... args) { return Messages.get(CoopScene.class,key,args); }

    @Override public void create() {
        super.create();
        if(active==null) active=CoopPlatform.controller();
        CoopDiagnostics.watch(CoopPlatform.root());
        controller=active;
        renderFrame();
        fadeIn();
    }
    @Override public void update() {
        super.update();
        if(controller==null) return;
        if(controller.frame().page==CoopController.Page.CLOSED) {
            if(exiting) { CoopDiagnostics.stopWatching(); active=null; controller=null; ShatteredPixelDungeon.switchNoFade(TitleScene.class); return; }
            String notice=controller.frame().notice;
            active=controller=CoopPlatform.controller();
            CoopDiagnostics.watch(CoopPlatform.root());
            if("CHECKPOINT_FAILED".equals(notice)) alert(msg("checkpoint_failed"));
        }
        CoopController.Frame next=controller.frame();
        if(shown!=next) {
            // A newly registered map PointerArea would otherwise take touches intended for an open Window.
            if(shown!=null && shown.page==next.page && com.watabou.input.PointerEvent.hasActivePress()) return;
            for(int i=members.size()-1;i>=0;i--) {
                Gizmo child=members.get(i);
                if(child instanceof Window) {
                    if(shown!=null && shown.page==next.page) return;
                    ((Window)child).hide();
                }
            }
            renderFrame();
        }
    }
    public static void stopSession() { if(active!=null) active.shutdown(); }
    @Override public void onPause() { if(controller!=null) controller.shutdown(); }
    @Override protected void onBackPressed() {
        if(targetView!=null) { cancelAim(); return; }
        if(shown.page==CoopController.Page.MENU) { exiting=true; controller.shutdown(); }
        else if(shown.page==CoopController.Page.GAME || shown.page==CoopController.Page.LOBBY) gameMenu();
        else controller.menu();
    }
    private void renderFrame() {
        CoopController.Frame f=controller.frame();
        if (board!=null && hud!=null && f.page==CoopController.Page.GAME && f.dungeon!=null && board.sameFloor(f.dungeon)) {
            shown=f;
            if (f.dungeon!=targetView || f.pending!=0) { targetView=null; targetStaff=null; targetMove=null; }
            game(f);
            return;
        }
        if(panel!=null) {remove(panel); panel.destroy();}
        panel=new Group(); addToBack(panel);
        if(hud!=null) { remove(hud); addToBack(hud); }
        if(board!=null) { remove(board); addToBack(board); }
        if(shown==null || shown.page!=f.page) first=0;
        shown=f;
        if (f.dungeon!=targetView || f.pending!=0) { targetView=null; targetStaff=null; targetMove=null; }
        com.watabou.utils.RectF inset=getCommonInsets();
        width=Math.min(212,Camera.main.width-inset.left-inset.right-8);
        center=(Camera.main.width+inset.left-inset.right)/2f;
        left=center-width/2; top=inset.top+3; bottom=Camera.main.height-inset.bottom-2;
        if (board!=null && (f.dungeon==null || f.page!=CoopController.Page.GAME || !board.sameFloor(f.dungeon))) {
            remove(board); board.destroy(); board=null;
            if (hud!=null) { remove(hud); hud.destroy(); hud=null; }
        }
        if(f.page==CoopController.Page.GAME && f.image!=null) { game(f); return; }
        panel.add(new ColorBlock(Camera.main.width,Camera.main.height,0xFF18151B));
        Image backdrop=new Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Splashes.SEWERS);
        backdrop.scale.set(Math.max(Camera.main.width/backdrop.width(),Camera.main.height/backdrop.height()));
        backdrop.x=(Camera.main.width-backdrop.width())/2; backdrop.y=(Camera.main.height-backdrop.height())/2;
        backdrop.brightness(0.38f); panel.add(backdrop);
        top+=Math.max(0,(bottom-top-142)/2);
        NinePatch frame=Chrome.get(Chrome.Type.WINDOW);
        frame.x=left-5; frame.y=top-3; panel.add(frame); top+=3;
        label(msg("title"),10,Window.TITLE_COLOR);
        if(!f.notice.isEmpty()) label(status(f.notice),6,0xE5B77D);
        switch(f.page) {
            case MENU:
                button(msg("host"),()->controller.host());
                button(msg("find"),()->controller.discover());
                button(msg("direct"),()->address("",null,false));
                pair(msg("saves"),()->controller.saved(false),msg("reconnect"),()->controller.saved(true));
                button(msg("diagnostics"),this::diagnostics);
                break;
            case ROOMS: case SAVES: case TICKETS:
                label(msg(f.page==CoopController.Page.ROOMS?"rooms":f.page==CoopController.Page.SAVES?"saves":"reconnect"),8,0xEEEEEE);
                if(f.list.isEmpty()) label(msg("empty"),7,0xAAAAAA);
                for(int i=first;i<Math.min(first+3,f.list.size());i++) {
                    CoopController.Entry e=f.list.get(i);
                    String name=e.id.toString().substring(0,8);
                    button(name+"  "+(f.page==CoopController.Page.SAVES ?
                            (e.current==null?msg("damaged"):msg("floor",e.current.depth,e.current.generation)) : e.address),()->entry(e));
                }
                if(f.list.size()>3) pair("‹",()->{first=Math.max(0,first-3);renderFrame();},"›",()->{
                    if(first+3<f.list.size()) first+=3;renderFrame();});
                if(f.page==CoopController.Page.ROOMS) button(msg("direct"),()->address("",null,false));
                break;
            case LOBBY:
                label(f.session==null?"":f.session.toString().substring(0,8)+"   "+f.address,7,0xF3DCB1);
                String[] roster=f.roster.split(" ");
                for(int i=0;i<2;i++) {
                    String[] member=roster.length>i+2?roster[i+2].split(":"):new String[0];
                    String name=member.length==3 ? (member[1].equals("null")?msg("select"):HeroClass.valueOf(member[1]).title()) : msg("waiting");
                    label(msg(i==0?"hostseat":"guestseat")+"  "+name+(member.length==3 && member[2].equals("1")?" / "+msg("ready"):""),7,0xCCCCCC);
                }
                float half=(width-2)/2;
                hero(HeroClass.WARRIOR,left,half); hero(HeroClass.MAGE,left+half+2,half); top+=20;
                hero(HeroClass.ROGUE,left,half); hero(HeroClass.HUNTRESS,left+half+2,half); top+=20;
                hero(HeroClass.DUELIST,left,half); hero(HeroClass.CLERIC,left+half+2,half); top+=20;
                boolean ready=roster.length>2+f.seat && f.seat>=0 && roster[2+f.seat].endsWith(":1");
                button(msg(ready?"unready":"ready"),()->controller.ready(!ready));
                if(f.seat==0) button(msg("start"),()->controller.start()).enable(roster.length>1 && roster[1].equals("READY"));
                break;
            case EPOCH:
                label(msg("rollback"),7,0xF1B66A);
                button(msg("accept"),()->controller.acceptEpoch()); break;
            case DISCONNECTED:
                if(f.session!=null) button(msg("reconnect"),()->controller.reconnect(f.session,""));
                button(msg("saves"),()->controller.saved(false)); break;
            default: label(status(f.page.name()),8,0xDDDDDD); break;
        }
        StyledButton back=make(msg(f.page==CoopController.Page.MENU?"titleback":"back"),this::onBackPressed);
        back.setRect(left,Math.min(top+2,bottom-16),width,16);
        frame.size(width+10,back.bottom()-frame.y+5);
    }
    private void hero(HeroClass cls,float x,float w) {
        StyledButton btn=make(cls.title(),()->controller.select(cls.name()));
        btn.icon(new Image(cls.spritesheet(),0,15,12,15));
        btn.setRect(x,top,w,18);
    }
    private void entry(CoopController.Entry e) {
        if(shown.page==CoopController.Page.ROOMS) controller.join(e.address,e.id);
        else if(shown.page==CoopController.Page.TICKETS) address(e.address,e.id,true);
        else {
            String body=e.current==null ? msg("damaged") : msg("savedinfo",e.current.depth,e.current.generation,
                    e.current.hostClass,e.current.guestClass,java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(e.current.savedAt)));
            String[] options=e.current!=null && e.backup!=null ? new String[]{msg("resume"),msg("backup")} :
                    e.current!=null ? new String[]{msg("resume")} : e.backup!=null ? new String[]{msg("backup")} : new String[]{};
            add(new WndOptions(msg("saves"),body,options) {
                protected void onSelect(int n) {
                    if(e.current!=null && n==0) controller.resume(e.id,false);
                    else confirm(msg("rollback")+" "+msg("floor",e.backup.depth,e.backup.generation),()->controller.resume(e.id,true));
                }
            });
        }
    }
    private void address(String initial,UUID id,boolean reconnect) {
        add(new WndTextInput(msg("direct"),msg("address"),initial,255,false,msg("connect"),msg("cancel")){
            @Override public void onSelect(boolean ok,String value) {
                if(ok) { if(reconnect) controller.reconnect(id,value); else controller.join(value,id); }
            }
        });
    }
    private StyledButton make(String text,Runnable click) {
        StyledButton b=new StyledButton(Chrome.Type.GREY_BUTTON_TR,text,8) {
            protected void onClick() { click.run(); }
        };
        panel.add(b); return b;
    }
    private StyledButton button(String text,Runnable click) {
        StyledButton b=make(text,click); b.setRect(left,top,width,16); top+=18; return b;
    }
    private void pair(String l,Runnable a,String r,Runnable b) {
        float half=(width-2)/2;
        make(l,a).setRect(left,top,half,16); make(r,b).setRect(left+half+2,top,half,16); top+=18;
    }
    private void label(String text,int size,int color) {
        RenderedTextBlock block=renderTextBlock(text,size);
        block.maxWidth((int)width); block.hardlight(color);
        block.setPos(center-block.width()/2,top); align(block); panel.add(block); top+=block.height()+3;
    }
    static String status(String code) {
        String text=msg("state_"+code.toLowerCase(java.util.Locale.ROOT));
        return text.equals(Messages.NO_TEXT_FOUND) ? code.replace('_',' ') : text;
    }
    private void confirm(String body,Runnable yes) {
        add(new WndOptions(msg("confirm"),body,msg("accept"),msg("cancel")){
            protected void onSelect(int n){if(n==0)yes.run();}
        });
    }
    private void alert(String body) {add(new WndOptions(msg("title"),body,msg("back")));}
    boolean hasOpenWindow() {
        for (Gizmo child:members) if (child instanceof Window) return true;
        return false;
    }
    void openBag(PublicDungeon view) {
        add(new com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatInventory(view,controller));
    }
    private String lastEnding = "";

    private void ascentWindow(CoopController.Frame f) {
        Class<?> amulet=com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class;
        boolean proposed="ASCENT_PROPOSED".equals(f.image.rows.get("ending"));
        add(new WndOptions(Messages.get(amulet,"ascent_title"), Messages.get(amulet,"ascent_desc"),
                Messages.get(amulet,"ascent_yes"),Messages.get(amulet,"ascent_no")) {
            protected void onSelect(int index) {
                if (index==0 || proposed) controller.act(f.image,index==0?"START_ASCENT":"CONTINUE_RUN",0,0);
            }
        });
    }

    private void endingWindow(CoopController.Frame f) {
        String phase=f.image.rows.get("ending");
        if ("ASCENT_PROPOSED".equals(phase) || f.image.rows.containsKey("ascentgate")) { ascentWindow(f); return; }
        boolean owner=Integer.toString(f.seat).equals(f.image.rows.get("amuletowner"));
        boolean canAccept=phase.equals("PROPOSED") ? !owner : owner;
        String[] options=canAccept ? new String[]{msg("end_vote"),msg("end_continue")} : new String[]{msg("end_continue")};
        add(new WndOptions(new Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Sprites.AMULET),
                msg("end_title"),msg(phase.equals("PROPOSED")?"end_proposed":"end_offer"),options) {
            protected void onSelect(int index) {
                controller.act(f.image,canAccept && index==0 ? "END_RUN" : "CONTINUE_RUN",0,0);
            }
        });
    }

    private void crownWindow(PublicDungeon view, PublicDungeon.Loot crown) {
        add(new com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseAbility(HeroClass.valueOf(view.heroClass),
                new com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseAbility.SeatChoice() {
            public void show(Window window) { CoopScene.this.add(window); }
            public void select(int index) {
                controller.act(view.source,crown==null ? "CLAIM_ARMOR" : "USE_CROWN",index,crown==null ? 0 : crown.id);
            }
        }));
    }

    void subclassAction(PublicDungeon view) {
        if (view.offered && view.source.rows.containsKey("weaponswap")) {
            controller.act(view.source,"SWAP_WEAPONS",view.pos,0);
        } else if (view.offered && view.source.rows.containsKey("monkenergy")) {
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.ArrayList<String> verbs=new java.util.ArrayList<>();
            for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility ability : com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility.abilities) {
                if (view.source.rows.containsKey(ability.seatAction().substring(5).toLowerCase(java.util.Locale.ROOT))) { names.add(ability.name()); verbs.add(ability.seatAction()); }
            }
            names.add(msg("back"));
            add(new WndOptions(Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.class,"name"),
                    msg("monk_energy",view.source.rows.get("monkenergy")),names.toArray(new String[0])) {
                protected void onSelect(int choice) {
                    if (choice<0 || choice>=verbs.size() || controller.frame().image!=view.source) return;
                    String action=verbs.get(choice);
                    if (action.equals("MONK_FOCUS") || action.equals("MONK_MEDITATE")) controller.act(view.source,action,view.pos,0);
                    else { targetView=view; targetMove=action; targetStaff=null; renderFrame(); }
                }
            });
        } else if (view.source.rows.containsKey("combocount")) {
            add(new WndCombo(Integer.parseInt(view.source.rows.get("combocount")),Integer.parseInt(view.source.rows.get("comboenhanced")),
                    move -> view.offered && view.source.rows.containsKey("finish"+move.ordinal()), move -> {
                if (controller.frame().image != view.source) return;
                if (move == Combo.ComboMove.PARRY) controller.act(view.source,move.name(),view.pos,0);
                else { targetView=view; targetMove=move.name(); renderFrame(); }
            }));
        } else if (view.offered && view.source.rows.containsKey("ambush")) {
            targetView=view; targetMove="ASSASSINATE"; renderFrame();
        } else if (view.offered && view.source.rows.containsKey("freerun")) controller.act(view.source,"FREERUN",view.pos,0);
        else if (view.offered && view.source.rows.containsKey("rage")) controller.act(view.source,"BERSERK",view.pos,0);
        else if (view.offered && view.source.rows.containsKey("snipercell")) {
            targetView=view; targetMove="SNIPER_SHOT"; renderFrame();
        }
    }

    private void subclassWindow(PublicDungeon view, PublicDungeon.Loot mask) {
        add(new WndChooseSubclass(new TengusMask(), HeroClass.valueOf(view.heroClass), new WndChooseSubclass.SeatChoice() {
            @Override public void show(Window window) { CoopScene.this.add(window); }
            @Override public void choose(HeroSubClass choice) {
                controller.act(view.source,mask == null ? "CLAIM_SUBCLASS" : "USE_MASK",choice.ordinal(),mask == null ? 0 : mask.id);
            }
        }));
    }

    void heroDetails(PublicDungeon view, int armorTier, String stats) {
        String title = Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndHero.class, "talents");
        HeroClass type = HeroClass.valueOf(view.heroClass);
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(title);
        int maskIndex = -1, crownIndex = -1;
        if (view.offered && view.source.rows.containsKey("maskright")) { maskIndex=options.size(); options.add(msg("use_mask")); }
        if (view.offered && view.source.rows.containsKey("crownright")) { crownIndex=options.size(); options.add(msg("use_crown")); }
        final int mask=maskIndex, crown=crownIndex;
        options.add(msg("back"));
        add(new WndOptions(com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite.avatar(type, armorTier),
                type.title(), "HP "+view.hp+"/"+view.maxHp+"\n"+stats
                        +(view.source.rows.containsKey("secrethint") ? "\n"+Messages.get(GameScene.class,"secret_hint") : ""),
                options.toArray(new String[0]) ) {
            @Override protected void onSelect(int index) {
                if (index == 0) showTalents(view, title);
                else if (index>=0 && index==mask) subclassWindow(view,null);
                else if (index>=0 && index==crown) crownWindow(view,null);
            }
        });
    }

    private void showTalents(PublicDungeon view, String title) {
        java.util.ArrayList<Integer> tiers = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (int tier = 1; tier <= 4; tier++) {
            boolean present = false;
            for (Talent talent : Talent.values()) {
                if (Integer.toString(tier).equals(view.source.rows.get("tier"+talent.ordinal()))) {
                    present = true;
                    break;
                }
            }
            if (present) {
                tiers.add(tier);
                labels.add(msg("tier_points", tier, Integer.parseInt(view.source.rows.get("freepoints"+tier))));
            }
        }
        labels.add(msg("back"));
        add(new WndOptions(title, "", labels.toArray(new String[0])) {
            @Override protected void onSelect(int index) {
                if (index >= 0 && index < tiers.size()) showTalentTier(view, title, tiers.get(index));
            }
        });
    }

    private void showTalentTier(PublicDungeon view, String title, int tier) {
        java.util.ArrayList<Talent> choices = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (Talent talent : Talent.values()) {
            String invested = view.source.rows.get("learn"+talent.ordinal());
            if (invested == null || !Integer.toString(tier).equals(view.source.rows.get("tier"+talent.ordinal()))) continue;
            choices.add(talent);
            labels.add(view.source.rows.get("tier"+talent.ordinal())+" · "+Messages.titleCase(talent.title())+"  "+invested+"/"+talent.maxPoints());
        }
        labels.add(msg("back"));
        int available = Integer.parseInt(view.source.rows.get("freepoints"+tier));
        add(new WndOptions(title, msg("tier_points",tier,available), labels.toArray(new String[0])) {
            @Override protected void onSelect(int index) {
                if (index < 0 || index >= choices.size()) return;
                Talent talent = choices.get(index);
                int points = Integer.parseInt(view.source.rows.get("learn"+talent.ordinal()));
                WndInfoTalent.TalentButtonCallback invest = null;
                if (view.offered && Integer.parseInt(view.source.rows.get("freepoints"+view.source.rows.get("tier"+talent.ordinal()))) > 0 && points < talent.maxPoints()) {
                    invest = new WndInfoTalent.TalentButtonCallback() {
                        @Override public String prompt() { return Messages.get(WndInfoTalent.class, "upgrade"); }
                        @Override public void call() { controller.act(view.source, "INVEST_TALENT", talent.ordinal(), 0); }
                    };
                }
                CoopScene.this.add(new WndInfoTalent(talent, points, invest, false, talent.icon(HeroClass.valueOf(view.heroClass))));
            }
        });
    }

    void gameMenu() {
        boolean host=shown.seat==0;
        boolean save=host && shown.page==CoopController.Page.GAME;
        boolean hasAmulet=board!=null && ("PROPOSED".equals(shown.image.rows.get("ending"))
                || Integer.toString(shown.seat).equals(shown.image.rows.get("amuletowner")));
        add(new WndOptions(msg("menu"),shown.address+"\n"+status(shown.image==null?"LOBBY":shown.image.rows.get("status")),
                board==null ? new String[]{msg("continue"),msg("diagnostics"),msg(host?"closeroom":"leave")} :
                hasAmulet ? new String[]{msg("continue"),msg("diagnostics"),msg(save?"saveexit":"leave"),msg("center_view"),msg("zoom_in"),msg("zoom_out"),msg("end_title")} :
                new String[]{msg("continue"),msg("diagnostics"),msg(save?"saveexit":"leave"),msg("center_view"),msg("zoom_in"),msg("zoom_out")}){
            protected void onSelect(int n) { if(n==1) diagnostics(); if(n==2) controller.leave(save); if(board!=null && n==3) board.centerOnHero(); if(board!=null && (n==4 || n==5)) board.zoom(n==4?1:-1); if(hasAmulet && n==6) endingWindow(shown); }
        });
    }
    private void diagnostics() {
        String report=CoopDiagnostics.report(shown);
        add(new WndOptions(msg("diagnostics"),report,msg("export"),msg("back")){
            protected void onSelect(int n){ if(n==0) CoopPlatform.export(report); }
        });
    }
    private void game(CoopController.Frame f) {
        PublicDungeon v=f.dungeon;
        if(v==null && "WON".equals(f.image.rows.get("status"))) {
            label(msg("ASCENDED".equals(f.image.rows.get("ending"))?"ascent_won":"end_won"),12,Window.TITLE_COLOR);
            Image amulet=new Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Sprites.AMULET);
            amulet.x=center-amulet.width()/2; amulet.y=top; panel.add(amulet); top+=amulet.height()+4;
            button(msg("menu"),this::gameMenu);
            return;
        }
        if(v==null) {
            label(status(f.image.rows.get("status")),10,Window.TITLE_COLOR);
            if(f.image.rows.containsKey("reason")) label(status(f.image.rows.get("reason")),8,0xF6BD89);
            button(msg("menu"),this::gameMenu); return;
        }
        String ending=f.image.rows.get("ending");
        if (ending!=null && ("OFFER".equals(ending) || "PROPOSED".equals(ending) || "ASCENT_PROPOSED".equals(ending))
                && !ending.equals(lastEnding) && "IN_GAME".equals(f.image.rows.get("status"))
                && ("ASCENT_PROPOSED".equals(ending) && !Integer.toString(f.seat).equals(f.image.rows.get("amuletowner")) || "PROPOSED".equals(ending) || Integer.toString(f.seat).equals(f.image.rows.get("amuletowner")))) {
            endingWindow(f);
        }
        lastEnding=ending==null ? "" : ending;
        String effect="";
        boolean advanced=f.delta && effectsVersion!=0 && f.image.version==effectsVersion+1;
        if(advanced) effect=f.cue;
        effectsVersion=f.image.version;
        String sound=null;
        switch(effect) {
            case "PICK_UP": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ITEM;break;
            case "EAT": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.EAT;break;
            case "ZAP": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ZAP;break;
            case "HURT": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.HIT;break;
            case "KILL": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.DEATH;break;
            case "REVIVE": case "REVIVED": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.TELEPORT;break;
        }
        if(sound!=null) com.watabou.noosa.audio.Sample.INSTANCE.play(sound);
        if(board==null) { board=new CoopBoard(v,this::touchCell); addToBack(board); }
        else board.refresh(v,effect,advanced);
        String line=f.pending!=0 ? ("TRAVEL".equals(f.pendingVerb)?msg("travelling"):msg("pending",f.pending)) : f.image.rows.containsKey("reason") ?
                status(f.image.rows.get("reason")) : !"ALIVE".equals(f.image.rows.get("life")) ?
                status(f.image.rows.get("life")) : msg(v.offered?"yourturn":"theirturn");
        if (!"0".equals(f.image.rows.get("keys")) && f.image.rows.containsKey("keys"))
            line+=" · "+msg("ironkeys",Integer.parseInt(f.image.rows.get("keys")));
        if(v.ally>=0 && "DOWNED".equals(f.image.rows.get("allylife"))) line+=" / "+msg("reviveally");
        if(f.lastResult.startsWith("REJECT") || f.lastResult.equals("UNKNOWN")) line+=" · "+status(f.lastResult);
        if (hud==null) { hud=new CoopHud(this,controller,getCommonInsets()); addToBack(hud); remove(board); addToBack(board); }
        String warning=f.image.rows.get("vaultwarning");
        if (f.image.rows.containsKey("vaultinjured")) warning="injure_warning_1";
        if (warning==null) warning="";
        if (!warning.isEmpty() && !warning.equals(lastVaultWarning)) {
            Class<?> source=warning.startsWith("injure") ? com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.class
                    : com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom.class;
            add(new com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage(
                    new com.shatteredpixel.shatteredpixeldungeon.sprites.ImpSprite(),
                    Messages.titleCase(Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class,"name")),
                    Messages.get(source,warning)));
        }
        lastVaultWarning=warning;
        hud.apply(v,f.pending==0,targetView!=null ? msg("aim") : line,targetView!=null);
    }
}
