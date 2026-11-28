# 협동 게임 동기화 프로토콜

## SYNC42 배포 후보와 SYNC43 개발 브랜치

SYNC42는 감정한 자기 재료에 `exoticrecipe<핸들>`로 원본 결과 번역 키·아이콘·비용을 표시한다. `BREW_EXOTIC`의 입력은 인접 솥 칸과 재료 핸들이다. 원본 심안 물약 비용 4와 공포 주문서 비용 6을 같은 파티 에너지에서 차감한다. `augmentgear<핸들>`은 소유자의 장비 순번·분류(W/A)·기존 증강을 담고, `AUGMENT_GEAR`는 그 순번 × 3 + 원본 증강 번호와 돌의 핸들을 보낸다. 원본 투척형 돌은 `THROW`, 감정한 지원 물약은 `DRINK`, 정화 물약의 파쇄는 `THROW`를 사용한다. 미감정 주문서의 실제 번역 키는 해당 좌석의 공개 물품 상세에 포함하지 않는다.

SYNC43 개발판에서 마법 탐지는 `DETECT_MAGIC <장비 순번> <돌 핸들>`이다. 직감은 `GUESS_ITEM <장비 순번 × 32 + 후보 번호> <돌 핸들>`이다. 소유자 투영의 `detectgear<핸들>`, `guessgear<핸들>`이 적격 장비를 나타낸다. `guess<계열><번호>`는 원본 공통 식별 지식에서 얻은 미지의 후보 이름으로, 클래스 이름순이며 정답 표시는 없다. 호스트가 행동권, 버전, 물리적 돌, 자기 장비, 현재 후보를 다시 확인한다. 선택과 취소는 클라이언트 입력이며 확정 행동만 돌과 시간을 쓴다. `runereading`은 사용자 좌석에만 주는 마지막 확정 턴과 여섯 결과 중 하나다. `curseknown<핸들>`은 이미 알려진 저주 여부다. 반지 종류 지식은 정확한 강화값을 공개하지 않는다.

## 2026-11-27 세 재료 연금술과 수혈

SYNC41의 세 재료 솥·유료 조합·수혈 지팡이와 실제 저장 경계는 [개발 결과](seed-alchemy-results.md)에 기록했다. SYNC40 배포 후보의 두 Android 필수 인수는 92/92였다. [Android 인수](cauldron-android-acceptance.md)

### SYNC41의 행동 및 공개 필드

* `BREW_SEEDS <솥 칸> <핸들1>_<핸들2>_<핸들3>`는 행동 본문의 마지막 세 필드다. 각 핸들은 앞에 0이 없는 양의 64비트 십진 정수다. 다른 행동은 기존 단일 핸들을 사용한다. 호스트가 현재 좌석의 공개 핸들을 물리적 재료로 해결하고, 같은 스택의 요구 수량을 합산한다. 솥 위치·행동권·버전·소유권을 재검사하고 세 단위와 원본 결과 한 개를 같은 영수증에 확정한다. 미리보기는 일반 물약 아이콘만 사용한다.
* `brewseed<핸들>=1`은 재료 선택 가능 여부다. `brewexotic<핸들>=6`은 보이는 인접 솥에서 자기 가방의 감정된 강화 주문서로 조합할 수 있음을 뜻한다. `BREW_ENCHANTMENT` 성공 시 공유 에너지 6과 그 주문서 한 장을 소비한다. 중복 요청은 기존 영수증을 반환하고 오래된 버전으로 두 번째 조합을 요청하면 거부한다.
* `READ_SCROLL`은 미감정 주문서의 공통 동작이다. 원본 룬 외형·수량과 개인 물품 핸들을 표시하고, `label<핸들>`의 원본 번역 키는 감정한 뒤에만 제공한다. 솥의 미감정 일반 주문서 미리보기에는 `unknown`, 룬석 자리표시자, 수량 2, 비용 0이 들어간다. 전체 및 델타의 물품 필드는 같은 좌석별 투영에서 생성된다. 공유 식별 지식과 개인 Trinity 발견 이력은 별도로 저장한다.
* 수혈은 기존 `ZAP`에 대상 칸과 자기 지팡이 또는 스태프 핸들을 사용한다. 실제 매혹은 시전자 Actor ID를 저장한다. 전투 마법사의 근접 효과로 얻은 `freeCharge`는 다음 회복에 드는 시전자 체력 비용만 면제한다. 일반 발사 충전과 저장된 지팡이 ID는 기존 규칙을 따른다.

## SYNC40 연금술 솥과 개인 기억

보이는 인접 솥은 cauldron<cell>, 자기 재료의 원본 미리보기는 brew<handle> 행에 전달된다. 미감정 룬석 결과는 unknown과 원본 자리표시자 아이콘으로 표시한다. BREW_RUNESTONES에 솥 칸과 자기 물품 핸들을 제출하며, 현재 State Version과 실제 물품 ID를 다시 검사한다. 미감정 주문서는 공통 READ_SCROLL을 사용하고 처리된 뒤 원본 식별 지식이 갱신된다. 분해 지팡이의 개인 발견 코드는 -112다. 시전자 ID와 숨은 가스 데이터는 전송 행에 추가하지 않았다. LAN 프레임 v1과 기존 소유자 전용 stasisheld를 유지한다. [검증](cauldron-results.md)

## SYNC39 보관 소환수와 각인

성서 소유자의 보관 상태를 stasisheld 행으로 보낸다. 활성 PowerBuff용 poweredspells와 독립적이며 다른 좌석의 보관 여부는 표시하지 않는다. READ_LULLABY는 식별된 자기 주문서를 확인하고 RECALL_INSCRIPTION은 자기 UsedItemTracker와 성서 충전을 확인한다. 개인 Blooming 발견 코드는 -111이다. LAN 프레임은 v1이다. [검증 결과](original-items-results.md)

## 2026-11-27 SYNC37 배포와 SYNC38 개발

SYNC38은 STASIS의 성서 입력과 개발 시험방의 비콘 동의 목적지 깊이·분기를 전송한다. 공개 warpdestination 행에는 휴면 층의 지도나 주민 정보가 없다. 착용 장비의 원본 후보는 소유 좌석에 표시하며 제한 후보에는 enchantlimited 행을 보낸다. LAN v1을 유지한다. [Cloud 검증](stasis-warp-results.md)

## SYNC37 장비 선택과 시야

LAN v1에서 MODE SYNC37을 협상한다. READ_SCROLL, ENCHANT_TARGET, ENCHANT_CHOOSE, ENCHANT_CANCEL은 현재 행동권·좌석·영수증을 확인한다. 원본 후보는 호스트가 확정한다. 해당 좌석에만 enchantphase(inventory/consumed/options), enchantgear<핸들>, enchantarmor, enchantoption0~2를 보낸다. 재인증·전체/델타 스냅샷에도 같은 공개 경계를 적용한다.

매로 직접 관측하는 칸은 소유자의 시야에 합치고, EAGLE_EYE의 벽 뒤 감지는 기존 별도 감지 행으로 보낸다. staffreach는 자기 장비의 근접 사거리, remotebeacon은 자기 비콘 조준, ascendedspells와 poweredspells는 자기 성서 주문 사용 조건을 드러낸다. 이 공개 표시만으로 다른 좌석의 표적이나 권한을 얻지 않는다. [현재 검증](enchantment-results.md)

## SYNC36 자기 활·아군·성직자 지형

LAN 프레임은 v1이다. 호스트와 클라이언트가 SYNC36에서 합의해야 신규 동작을 전송한다. ENCHANT_BOW는 자기 돌의 공개 핸들과 자기 활의 목록 번호를 전달한다. 호스트가 두 실물의 소유권을 검사하고 원본 무작위 선택을 확정한다. bowtarget은 자신의 가방에 속하는 활 선택 번호다. bowenchant 및 bowlimited는 원본이 공개하는 마법에 한해 이름과 지원 여부를 나타낸다. 조준·창 취소는 RPC를 발행하지 않는다.

warpally는 개인 시야에 직접 보이는 적격 자기 소환수의 칸에 붙는다. 수혜자·실제 거리·점유와 착지는 호스트가 다시 확인한다. lightwall은 관측된 광벽의 칸이고 endlightwall은 시전자에게만 보내는 자기 벽 해제 상태다. 지팡이가 없는 Trinity 서리 기억은 기존 좌석 전용 발견 목록의 -108을 쓴다. 다른 좌석의 발견·가방·조준 상태는 전송하지 않는다. [실행 결과](shared-growth-results.md)

## SYNC35 공개 행과 입력

기존 learn/tier 및 개인 발견 행을 사용한다. 마력탄 기억은 discovery107과 선택값 -107이다. CLEANSE는 시전자 자신, HOLY_LANCE는 자기 성서와 직접 관측한 유효 적을 확인한다. SEER_SHOT은 원본 활을 가진 영웅이 자기 직접 시야의 빈 칸에 SHOOT를 보낼 수 있다. 직접 시야와 감지·기억은 독립적이며 감지만으로 공격 표적을 허용하지 않는다. [실측과 범위](tier-growth-results.md)

## SYNC34

일반 LAN 프레임은 v1이다. 좌석은 식별된 자기 가방 재충전 주문서를 RECHARGING_SCROLL로 받아 READ_RECHARGING을 요청한다. 물품 핸들·행동권·상태 버전을 서버에서 확인한다. 자기 UsedItemTracker가 이 주문서를 기억하는 동안에만 recallrecharging=1을 보낸다. RECALL_INSCRIPTION은 이 경우 자기 위치를 받으며 성서와 원본 각인 비용을 검사한다. 감정 주문서의 선택 핸들 경로도 유지한다. 개인 Trinity Blocking 선택은 discovery106과 대상 -106을 사용한다. 단일 도적 조건은 learn/tier 행과 실제 투자 양쪽에 적용한다. 배포 수정판은 SYNC32, 직전 개인 발견판은 SYNC33을 사용한다. [검증](cache-growth-results.md)

## SYNC33의 좌석별 Trinity 선택

성서와 실제 형상 특성을 가진 좌석에만 현재 자기 가방 밖에서 선택할 수 있는 발견 후보를 보낸다. discovery101은 Chilling, 102는 Swiftness, 103은 WandOfLightning, 104는 RingOfHaste, 105는 Blazing이다. 각 행의 값은 1이다. 소유한 적격 물품은 기존 bodyitem·minditem·spirititem 경로로 고른다. BODY_FORM·MIND_FORM·SPIRIT_FORM으로 기억을 선택할 때 셀 인수는 각각의 음수 후보 코드다. 성서 핸들, 자기 발견 이력, 형상, 행동권과 상태 버전을 호스트가 검사한다. 도적의 예지가 원본 판정을 통과하면 자기 공개 상태에 secrethint=1을 추가하고 캐릭터 창에서 원본 안내문을 표시한다. 이 행에는 방 위치나 후보 개수가 없다. 전달 프레이밍은 LAN v1이다. 기존 수정 APK의 협상값은 SYNC32다. [선택과 복원](trinity-discovery-results.md)

## SYNC32의 소유자 입력

LAN v1에서 양쪽이 SYNC32를 협상한다. 자기 가방의 `detachseal`, `affixseal`, `sealarmor`, 문양 행과 후보 순번으로 인장 선택을 구성한다. HolyIntuition·각인 회수·Trinity도 자기 가방의 현재 순번과 실제 아이템 핸들을 호스트가 다시 검사한다. 메뉴·취소는 로컬 UI에서 끝난다. 새 행동에는 기존 Action ID와 State Version을 사용한다. 빛의 아군과 잔상은 보이는 Actor로만 공개한다. 기존 직접 시야·감지 구분과 타인 물품의 비공개를 유지한다. [실행 범위](seal-arts-results.md)

## SYNC31과 좌석별 감지

SYNC30에서 여섯 기본 직업, `IMBUE`, 소유 무기의 `WEAPON_ABILITY` 및 성서의 기본 주문을 협상했다. 주입 후보 행은 자기 가방에서 고를 수 있는 순번만 제공하고 감정된 후보에만 강화값을 표시한다. 소유한 무기의 충전·부분 충전과 지팡이 보존 판정은 호스트가 확정한다. [SYNC30 검증](wand-arts-results.md)

SYNC31은 동일한 LAN v1 프레이밍에서 새 주문·무기·수도사 입력, 분신 명령 및 투영을 추가한다. `t<cell>`은 현재 관측한 지형, `r<group>`은 탐험·지도 기억이다. `sense<cell>=mob`은 그 좌석에게 위치가 감지된 주민, `terrain`은 감지된 주변 지형이다. 감지만으로 HP, 물품 핸들, 함정 또는 공격 가능한 직접 시야를 제공하지 않는다. 이동·만료 때 이전 감지 행을 델타에서 제거한다. 수신자에게 변화가 없는 델타도 직전 버전과 새 체크섬을 확인해야 한다.

`monkenergy`와 `monkfocus`는 자기 수도사의 현재 에너지와 집중 가능 여부를, `weaponswap`은 자기 용사의 무기 교체를 나타낸다. 손에 든 무기와 가방의 보조 무기, 성서, 갑옷은 인증된 좌석의 기존 아이템 핸들로 검증한다. 가면과 왕관 분배는 일반 방에서 각각 OFF다. [성장·감지 검증](frost-senses-results.md)

## SYNC29

기존 LAN v1 위에서 SYNC29를 협상한다. 자기 3단계 잔여 포인트와 허용 특성, 자기 장착 망토의 사용·은신 상태, 자신의 비콘 상태를 투영한다. CLOAK는 소유한 실제 망토 핸들이 필요하다. 능력의 지불·도달 가능성과 보이는 표적을 호스트에서 검사한다. [실제 입력](upper-growth-results.md)

## SYNC28 성장 입력

자기 좌석의 콤보 수, 남은 시간, 사용 가능한 마무리 기술, 분노 발동 가능 여부, 보이는 표식 셀, 단계별 특성 포인트와 갑옷 충전을 공개한다. finish 필드는 원본 기술의 순번을 사용한다. 상대의 장비나 보이지 않는 적의 위치는 포함하지 않는다. 클라이언트는 유형, 표적 셀과 기존 아이템 핸들을 보낸다. 호스트가 실제 버프·특성·행동권·사거리·충전을 검사한다. 특성 투자와 수동 광전사 발동은 원본 게임 시간을 소비하지 않는다. LAN은 v1이다.


## SYNC27 — 좌석별 다트와 성장 입력

플레이 세션은 SYNC27을 협상한다. 검색 및 로비는 LAN v1이다. 가방에는 DART의 비공개 참조와 THROW 동작이 있으며 확정된 공개 투척에는 DART 종류를 보낸다. 자기 좌석에만 freepoints1과 learn<특성 번호>를 보낸다. INVEST_TALENT는 자기 좌석의 유효한 1단계 포인트를 소비하고 같은 턴에 새 State Version과 영수증을 발행한다. 시험방의 USE_MASK는 자기 가방의 핸들, CLAIM_SUBCLASS는 보류된 자기 선택권을 요구한다. 두 요청 모두 선택한 원본 전직 번호를 전달한다.

SYNC26은 VAULT_RECALL과 소유 좌석의 비콘 사용 메뉴를 추가한다. Acidic을 공개 주민으로 해석하며 금고 발사 장치의 원본 경고는 보이는 발사자와 수신자가 보는 칸에만 표시한다. LAN은 v1이다. [검증 기록](vault-play-results.md)

SYNC25에서 START_ASCENT, ASCENT_PROPOSED, ASCENT_READY, ASCENDING, ASCENDED를 구분한다. ascentgate는 양쪽이 계단 범위에 있을 때 아뮬렛 소유자에게만 전달한다. aim은 수신 좌석의 시야로 제한한다. [실행 결과와 다음 작업](vault-ascent-results.md)

SYNC24는 ending과 amuletowner를 공개하고, 각 좌석에는 자기 시야에 들어온 광선 예고 셀만 보낸다. END_RUN과 CONTINUE_RUN은 인증된 Action ID와 최신 상태에 결합한다. 이미 발행된 영수증은 동일 결과를 반환한다. WON 이후 새 일반 행동은 REJECT_ENDING으로 거부한다. LAN 프레이밍 v1을 유지한다. [최종 보스·승리 검증 기록](yog-victory-results.md)

SYNC23은 branch 1의 도시 금고와 광산을 구별한다. 보이는 인접 NPC에는 vaultnpc, 탈출 가능 시 vaultscore, 소유자가 선택할 수 있는 물품에만 vaultpick 행을 제공한다. VAULT_INTERACT와 VAULT_REWARD는 일반 Action ID·좌석·State Version 검증을 거친다. [Imp 금고와 악마의 전당](imp-halls-results.md)

## 광산·도시 체크포인트

SYNC22에서 공개 floor의 branch와 depth가 함께 지도 식별자가 된다. 버전이 바뀌면 이전 분기에서 준비한 좌표 행동을 거부한다. 채굴, 장비 서비스와 의뢰 수락은 기존 Action ID 및 발행 경계를 따른다. [실행 기록](mines-city-results.md)

## SYNC21 · 동굴

LAN v1을 유지한다. 대장장이 수락은 ACCEPT_BLACKSMITH로 현재 좌석, 인접 NPC, 퀘스트와 소유권을 검증한다. 보스방 장식과 과충전, 전격·가스 셀은 기존 좌석별 지도와 시야를 따르고 새 스냅샷에 과거 공격을 재생하지 않는다. [동굴 실행 기록](caves-dm300-results.md)

## SYNC20 · 같은 깊이의 보스방 교체

공개 floor 행에는 10층에서 branch,depth,stage가 들어간다. stage는 START, FIGHT_START, FIGHT_PAUSE, FIGHT_ARENA, WON 중 하나다. floor 행이 달라지면 전체 이미지를 검증한 뒤 지형·시야·스프라이트를 교체한다. 원본 출구 장식의 exitfloor와 exitwall은 해당 좌석이 관찰한 셀만 전달한다. 현재 보이는 원본 함정 예고와 화염·전격은 지속 상태로 표시한다. 행동 검증에는 기존 턴·버전·영수증을 사용한다. LAN 발견 프로토콜은 v1이다.

## 이전 플레이 모드 SYNC19

양쪽 좌석은 SYNC19로 인증한다. 해당 좌석에서 보이는 강령술사의 소환 준비는 charge<현재 보이는 시전자 셀> 행으로 전달한다. 준비 셀과 소환 관계의 내부 Actor ID는 비공개다. 신규 해골·망령·박쥐도 관측된 셀에서만 표시한다. 휴대 충격파·화염 방사 지팡이는 자신의 감정된 비저주 소지품에 WAND, 원본 이미지 및 충전량을 표시한다. 조준 자체는 로컬 동작이며 ZAP의 효과와 시간은 호스트가 확정한다.

## 이전 플레이 모드 SYNC18

이전 MODE 요청은 ERROR VERSION으로 거부한다. 감정된 비저주 휴대 마력탄은 소유 좌석의 가방 행에서 WAND 종류와 현재/최대 충전량을 표시한다. ZAP에 최신 소유자 핸들·시야·충전·행동권 검사를 적용한다. 보상 확정과 시전은 서로 다른 행동 영수증을 가진다.

## 의뢰판 SYNC17

SYNC17에서 장인 수락 ACCEPT_WANDMAKER와 선택 WAND_FIRST·WAND_SECOND는 NPC 셀을 대상으로 한다. 공개 대화 상태 OFFER·HUNT·REWARD는 좌석에 따라 달라진다. 후자의 두 후보는 소지자에게만 공개한다. 이전 모드와의 연결은 버전 검사에서 거부한다.

## 이전 플레이 모드 SYNC16

MODE SYNC16을 사용하며 이전 SYNC15·14 요청은 ERROR VERSION으로 거부한다. SYNC15의 구매·판매·환매 요청과 가격 공개 범위를 이어받는다. THROW의 KNIFE는 소유자에게 보이는 기본 단검 연출에 해당한다. 현재 지원 깊이 상한은 7이다. 실제 층 변경 시 새 층의 전체 상태를 검증한 뒤 표시 모델을 교체한다.

## 이전 SYNC14 개인 조작 확장

소유 아이템의 `ref<handle>`은 좌석별 비공개 무작위 값과 실제 소유 객체 ID로 만든 128비트 표시값이다. 바닥이나 상대 좌석에는 발행하지 않는다. `pocket`·`bagtab`은 실제 자기 가방 위치를 담는다. 돌 세트의 `family`도 좌석 범위로 한정한다. 이 값에는 행동 권한이 없다.

`UPGRADE`와 `IDENTIFY`는 기존 DO의 item 필드로 주문서 핸들, cell 필드로 해당 공개 버전의 자기 장비 선택 순번을 받는다. `select<handle>`에는 이 순번과 공개 가능한 선택 가능 여부만 담는다. `THROW`는 cell을 지도 좌표로 사용한다. 서버는 실제 가방·양쪽 아이템의 귀속·버전·차례를 재검증한다. 캐시된 요청은 원래 영수증으로 해결한다. 공격·사격·투척의 자기 영수증에는 확인된 좌표를 넣을 수 있다. 이전 3필드 영수증도 렌더러에서 처리한다.

## 그래픽 표시용 SYNC13 선택 행

같은 모드에서 호스트가 자기 좌석의 보호막 합계 `shield`, 현재 층 높이 `height`를 제공한다. 구형 호스트에서 이 값이 없으면 표시 모델은 보호막 0 및 제한된 최대 높이를 사용한다. Goo 둥지의 `nest<cell>`은 직접 관측했거나 이전에 방문한 불변 장식의 원본 프레임 0–8이다. 미방문 매핑 셀에는 이를 보내지 않는다. `label<handle>`은 해당 좌석의 소지품 중 공개 가능한 종류의 번역 키이다. 일반 바닥 더미·상대 가방·정체를 모르는 물약의 클래스명은 이 행으로 전송하지 않는다. 모든 행은 기존 좌석 해시, 변경분 적용, 크기 제한 경계에 속한다.

## SYNC13 식물 표시

직접 관측 가능한 셀에만 `plant<cell>` 행이 생기며 값은 원본 terrain features atlas 7번 줄의 식물 열 번호다. 함정이 해당 셀의 표시를 차지하면 식물 행을 내보내지 않는다. 식물이 시들거나 직접 시야가 끊기면 다음 좌석별 델타에서 제거한다. 기억 지형에는 식물 행을 복사하지 않는다. 표시는 PublicDungeon의 검증된 StateImage에서 읽는다. 관련 입력은 기존 MOVE를 사용한다.


## 이전 체크포인트 SYNC13

호스트와 클라이언트는 MODE SYNC13을 협상한다. 상대 모드가 다르면 VERSION 오류로 끝난다. 영혼의 활 SHOOT와 식별된 회복 물약 DRINK는 좌석별 item ID, 소유권, Action ID 및 현재 턴을 검사한다. 갑옷 UI는 기존 EQUIP/UNEQUIP을 사용한다. 현재 보이는 Goo에 대해서만 charge셀에 원본 준비 단계를 싣는다. 5층까지 층 전환 때 새 floor 행과 수신자별 전체 스냅샷을 검증한다. 재접속 자격과 비공개 보관 아이템은 공개 상태에 포함하지 않는다.

### 이전 협상 규약 이력

## SYNC11 공개 상태

keys는 현재 층의 파티 철 열쇠 수량이다. h셀의 LOCKED_CHEST는 실제 잠긴 상자와 위장 중인 황금 미믹이 공유한다. 해당 셀은 발견 전 m·z·보상 행을 생성하지 않는다. 직접 관측하는 셀에서만 공개하는 trap셀 값은 원본 색+16×shape, cloud셀은 Fire=1, Freezing=2, Electricity=4, ToxicGas=8, ConfusionGas=16의 비트 합이다. 관측을 잃으면 동적 행을 지운다. Sheep과 모습을 드러낸 GoldenMimic은 직접 보이는 경우에만 표시한다.

## 방과 연결

호스트의 엔진 스레드는 `LanRoom.synchronizedGame(bind,tcp,udp,seed)`로 방을 만들고 pump한다. 각 클라이언트는 `LanClient.synchronizedGame(address)`로 연결한다. 기존 v1 길이 헤더와 HELLO 뒤에 MODE SYNC1을 보내고 같은 응답을 확인한 뒤 HOST 또는 JOIN을 보낸다. 일반 v1 방과 동기화 방은 서로의 참가 방식을 거부한다. 동기화 방에서는 기존 ACT를 쓸 수 없다. UDP 광고와 로비의 SELECT·READY·START는 Phase 4와 같다.

엔진은 두 영웅을 생성해 첫 입력 차례가 준비되면 버전 1을 확정한다. 이후 하나의 DO를 실행하고 다음 입력 차례가 준비됐을 때 양쪽 공개 상태를 함께 확정한다. 사이에 처리된 몹·버프·회복도 이 경계에 포함한다. TURN과 stateVersion은 별도 카운터다. 재생 시간 재조정, 조회·PING, 사전 검증 거부, 동일 결과 반환은 버전을 소모하지 않는다. 단절·엔진 중단에 따른 일시정지는 한 번 확정한다.

## 행동과 결과

```text
1 rid session DO player seat actor actionID baseVersion turn verb cell itemID
1 rid RESULT actionID seat actor requestedTurn requestedBase committedVersion outcome
```

바깥 rid는 연결마다 증가하는 전송 번호다. 동일 논리 행동을 재전송할 때 rid만 새 번호가 된다. actionID는 세션·좌석별 양의 long이다. 입력은 2,048바이트 이내이며 DO만 13개 토큰을 쓴다. 문자열 표현이 다른 숫자도 파싱한 수치로 비교한다. 동기화 방의 아이템 ID는 v1 STATE의 턴별 핸들과 구분한다.

1. 세션 UUID, 실제 연결의 좌석, player 및 해당 영웅의 Actor ID부터 확인한다.
2. 좌석별 최근 64개의 행동 기록에서 ID를 찾는다. 같은 base·turn·verb·cell·item이면 저장된 RESULT를 반환하고 다른 내용이면 ERROR CONFLICT다. 기록이 밀려났지만 처리한 최고 ID 이하인 요청은 ERROR EXPIRED다. 이전 세션이나 다른 좌석의 결과를 조회할 수 없다.
3. 새로운 ID면 버전·턴·소유권과 실제 CoopSession.act의 이동·공격·소비 조건을 검사한다. RESULT의 REJECT_VERSION/TURN/ITEM/INVALID/ACTION/PAUSED는 거부다. 인증된 새 ID의 거부 결과도 64개 한도에 포함된다. 사용하지 않은 작은 번호를 나중에 제출할 수 없다.
4. APPLIED는 정상 확정, APPLIED_STOPPED는 승인된 행동 후 엔진이 정상 중단 조건을 보고한 경우다. HALTED_UNCERTAIN은 실행이나 확정 중 예외가 발생해 일부 효과가 이미 반영됐을 수 있다는 뜻이다. 재실행이나 롤백을 시도하지 않는다. PAUSED 모델은 행동권과 플레이 중의 지도를 제거한다. 공개 사유는 DISCONNECTED, ENGINE_STOPPED, ENGINE_ERROR 중 하나이며 예외 전문은 호스트에만 둔다.

영수증에는 좌표·다른 사람의 인벤토리·전체 상태를 싣지 않는다. 마지막 확정 경계가 자신의 행동이면 자신의 모델에만 receipt 행이 들어간다. 이 캐시와 최고 ID는 방의 메모리에 있다. 캐시 밖의 과거 결과나 완전히 끊어진 연결에서의 좌석 복원은 지원하지 않는다.

## 상태 전송

| 요청 | 응답 |
|---|---|
| SYNC 0 NEW | 해당 좌석의 현재 전체 스냅샷 첫 PART |
| SYNC version sha256 | 같은 상태이면 CURRENT version sha256. 직전 버전과 해시가 일치하면 변경분 PART. 그 밖에는 RESYNC |
| PAGE transfer index | 같은 연결·좌석의 활성 전송에서 요청한 PART. 만료·새 확정 경계 또는 전송 교체 후에는 ERROR TRANSFER |

PART는 `PART transfer index count transferSHA256 segment`이다. 인덱스는 0부터 시작한다. 조각은 최대 1,400 ASCII 바이트, 최대 64개, 전체 최대 89,600바이트, 공개 상태 최대 2,048행이다. 마지막 조각을 제외한 길이는 일정하다. 좌석당 현재/직전 공개 상태와 전송 본문 하나만 유지한다. 조립 제한은 5초다. 커밋되면 두 좌석의 이전 전송을 무효화한다. 스냅샷 생성·변경분 생성은 불변 버전을 사용한다. TCP의 길이·속도·큐·열린 연결 제한은 기존 명세를 따른다.

본문은 `S|session|seat|0|version|stateSHA256|operations` 또는 `D|session|seat|base|version|stateSHA256|operations`이다. D는 한 커밋만 전진한다. 일반 동작은 키 단위로 달라진 값만 보낸다. 아주 큰 변경분은 상한 내 전체 스냅샷으로 대체할 수 있다. 연산은 세미콜론으로 끝난다. `+key=value`는 추가·교체, `-key`는 모델에서 제거한다. 몹의 -는 관측 상실이다. `!m<ID>`는 이전에 보였고 직접 근접 공격으로 처치가 확인된 몹이 같은 관측 위치에서 사라졌음을 표시한다. 시야 밖의 죽음은 보고하지 않는다.

## 공개 행

| 키 | 의미 |
|---|---|
| status, turn, active, clock | 세션 상태, 입력 차례, 현재 좌석(-1이면 없음), 원본 누적 시간과 Actor.now의 합 |
| reason, receipt | 일시정지 사유, 행동자 본인의 마지막 확정 경계에 대한 actionID·verb·outcome |
| width, self, ally | 셀 인덱스 해석용 폭. self는 actor·위치·HP·HT·직업·레벨·경험치. ally는 직접 볼 수 있을 때만 actor·위치·직업 |
| t셀, h셀 | 현재 직접 시야 안의 표시 지형, 내용물을 열람할 수 없는 더미의 일반 컨테이너 표기. 비밀 문은 벽, 숨은 함정은 빈 바닥으로 보냄 |
| mActorID | 현재 직접 관측 가능한 지원 몹의 위치·종류·HP·HT. 투명 몹 제외 |
| bActorID | 자신의 표시 아이콘이 있는 버프/디버프의 아이콘과 원본 표시용 남은 시간(FlavourBuff), 기타 NA |
| i좌석별ID | 소유·장착 슬롯 또는 현재 보이는 일반 바닥 더미의 맨 위 셀, 종류(FOOD/MELEE/OTHER), 표시 이미지·수량·알고 있는 강화 수치 |

직접 시야는 원본 Level의 차폐·거리 계산을 공유하며 특수 감지와 기존 탐험 배열을 합치지 않는다. 자기 칸은 포함한다. 아이템 ID는 좌석별로 발급하며 연속해서 공개되는 같은 객체는 바닥→가방→장비로 이동해도 같은 ID다. 스택에 흡수되거나 소모되거나 시야에서 사라지면 해당 좌석의 매핑을 제거한다. 재발견 시 새 ID를 받을 수 있다. 아이템의 ID와 행동 권한은 별도로 검사한다. 다른 사람의 장비·가방 내용, 미관측 지형, 숨은 몹, 월드 RNG, 방장 자격값은 공개 모델과 해시에서 제외한다.

## 표시 모델과 오류 처리

좌석마다 하나의 SeatActions와 DisplayReplica를 유지한다. refresh로 첫 상태를 받은 뒤 UI에는 불변 StateImage를 전달한다. 현재 좌석이라면 SeatActions.prepare로 Pending을 만들고 submit한다. 영수증이 애플리케이션에서 유실되면 같은 Pending을 같은 연결에서 다시 제출한다. 화면을 새로 동기화해도 SeatActions의 번호 생성기는 유지한다.

조각 순서·토큰·개수·시간·전송 해시를 확인한 뒤 임시 맵에 연산을 적용한다. 클라이언트는 **적용한 행**을 키 순서로 정렬해 `session|seat|version|key=value;...`의 SHA-256을 직접 계산한다. 호스트의 공개 상태 해시와 일치할 때만 모델을 교체한다. 동일 변경분은 재적용하지 않는다. 버전 누락·잘못된 순서·손상에는 전체 스냅샷을 한 번 새로 요청한다. 그 시도도 실패하면 마지막 정상 모델을 유지하고 호출자가 오류를 받는다. 실제 전송 연결이 끊겨 refresh가 통신 실패를 받으면 모델을 닫는다. 화면에서 주도적으로 퇴장할 때도 close를 호출한다.

플랫폼 UI, 플레이어별 영구 탐험 기록, 특수 감지, 장기 저장용 아이템 ID와 캐시, 재접속은 후속 단계다.

## Phase 6 좌석별 게임 규칙

SYNC1의 2,048바이트 요청 프레임, 1,400바이트 조각 64개, 상태 2,048행 및 조립 5초 한도를 유지한다. 좌석의 직접 시야·기억 지형·전투 경계가 같은 공개 상태 해시와 원자적 교체에 포함된다. 같은 방에서 전체 재동기화하면 호스트가 보관한 해당 좌석의 기억을 복원한다.

| 행 또는 행동 | 해석 |
|---|---|
| t셀 | 지금 관측하는 지형. 현재 보이는 몹·바닥 아이템 행은 시야를 잃으면 제거된다. 숨겨진 더미에는 아이템·컨테이너 행이 없다. |
| r그룹 | 연속 32셀의 현재 시야 밖 기억을 64 ASCII 문자로 표현한다. 셀 번호를 32로 나눈 몫이 그룹, 나머지가 그룹 안의 위치다. 두 문자 중 첫 문자는 직접 방문 v 또는 지도 효과만으로 파악한 p다. 둘째 문자는 숫자, 소문자, 대문자 순서의 지형 번호다. 빈 쌍은 점 두 개다. 전부 빈 그룹은 생략한다. 시야를 벗어난 몹이나 아이템은 기억 행에 기록하지 않는다. |
| z셀 | 공개된 통행 영역의 테두리. 북·동·남·서에 각각 비트 1·2·4·8을 배정한다. 전역 구역 번호, 참여자 명단, 전체 면적을 전송하지 않는다. |
| life, revivable, allylife | 자신의 ALIVE/DOWNED/DEAD 상태와 부활 가능성. 보이는 동료에 한해 allylife를 보낸다. REVIVING은 자원 소비와 복구 사이의 엔진 내부 상태다. |
| DO … DROP cell itemID | 현재 자신의 위치에 자신의 가방 속 지원 아이템을 한 단위 내려놓는다. 원본 드롭 시간을 사용한다. |
| DO … UNEQUIP 0 itemID | 자신의 지원 근접 무기 장착을 해제하고 가방에 넣는다. 원본 장비 해제 시간을 사용한다. |
| DO … REVIVE cell 0 | 보이는 인접 동료를 부활시킨다. 호스트가 대상, 점유, 자원 소유권을 확인한다. 다른 사람의 비공개 아이템 핸들을 전달할 필요가 없다. 자신의 인벤토리와 관측 가능한 바닥 더미에서는 앙크 종류를 ANKH로 표시한다. |
| PARTY_DEFEATED | 마지막 생존자가 쓰러진 행동의 확정 결과와 최종 status다. active는 -1이다. 그 뒤 새 행동은 REJECT_DEFEATED, 동일 행동의 재조회는 원래 영수증을 반환한다. 엔진 예외에는 기존 HALTED_UNCERTAIN을 사용한다. |

행동·소유권 검사는 CoopSession.act에서 v1과 공유한다. 기존 v1 ACT에서는 세 신규 동사를 ERROR MODE로 거부한다. SYNC1에서는 각 DO의 action ID·base·turn·cell·공개 핸들이 기존 캐시 규칙을 따른다. 투척과 직접 전달 명령은 지원하지 않는다.

## 디스크 복원 경계

영구 방의 모델에는 epoch가 포함된다. 재접속 대기 중에는 status=PAUSED, reason=RECONNECTING, active=-1과 해당 좌석이 확인할 수 있는 전체 공개 상태를 제공한다. 재접속용 새 DisplayReplica에서 전체 스냅샷을 확인하고 체크섬을 호스트에 제출한다. 복원과 단절은 공개 아이템 핸들을 무효화한다. 저장된 발급 상한부터 새 핸들을 할당한다. 영수증은 원본 요청의 핸들 문자열을 보존한다. [재접속 명세](reconnect.md)

## Phase 8 UI

selftier는 자신의 원본 방어구 외형 단계다. allytier는 ally가 공개되는 버전에만 포함한다. 기존 값의 부재는 기본 외형으로 표시할 수 있다. 공개 해시, 버전 및 청크 제한은 기존 SYNC1 규약을 따른다.

다른 좌석의 확정으로 페이지 전송이 바뀌면 클라이언트는 직전 검증 모델을 유지한다. DisplayReplica의 제한된 새 스냅샷 시도와 작업자의 후속 폴링을 사용한다. 유효하지 않은 전송이 세 폴링에 연속되면 연결 오류를 표시한다. 오래된 입력은 버전 검사를 거친다.

## 하수도 특수 방 체크포인트

SYNC6은 시야 안의 Sentry·Piranha·Ghost·FetidRat·Bee와 허용된 아이템 종류를 표시한다. charge<cell>은 충전 중인 감시탑을 직접 볼 때만, quest<cell>은 유령을 직접 보고 인접할 때만, stench<cell>은 현재 시야 안에 가스가 있을 때만 보낸다. 일반 객체와 같은 관측 상실·해시·재접속 전체 스냅샷 검사를 거친다. [메시지 및 동사](lan-protocol.md)

## 상자 위장과 몬스터 보관품

SeatProjection은 중립 미믹을 CHEST 또는 CRYSTAL_CHEST로 표시한다. 위장 상태에서는 해당 Actor의 m행·HP·전리품·공격 상태가 없다. PublicDungeon과 CoopBoard는 두 좌석에서 같은 h셀 입력을 사용한다. 공개 후 직접 볼 수 있는 동안 기존 m행의 위치·HP와 원본 텍스처를 사용한다. 몬스터 보관품의 내부 ID 및 소유자 필드는 디스크 저장에만 존재한다. 귀속품이 바닥으로 돌아오면 공개 item ID와 별개로 호스트가 실제 소유자를 검증한다.

### Shop checkpoint protocol SYNC15

The shop checkpoint used MODE SYNC15 and rejected older peers with ERROR VERSION. Its transaction rows and validation rules carry forward to SYNC16. Stock and price inspection use the authenticated seat's snapshot. A visible sale cell has saleN (original item image) and, for supported merchandise, quoteN (seat handle, native price, quantity, capacity flag). On floor 6 gold carries the party balance. Adjacent, observing seats get merchant (shopkeeper cell) and sellH (total value, single-unit sale eligibility) for their own eligible inventory. Only that seller gets refundH (image, quantity, value, capacity flag) for merchandise retained by the keeper.

BUY takes the stock cell and handle. SELL, SELL_ONE and BUYBACK take the merchant cell and a seat-scoped handle. All four reuse the ordinary turn, State Version, Action ID, item resolution and durable receipt path. Opening and cancelling a quote send no game action. A full bag, insufficient funds, mismatched location, ownership, expired offer or missing stock prevents the transaction. No client supplies a price.

Plain throwing knives use the existing THROW action. An identified owned knife has the KNIFE inventory kind and a seat-salted stack family. A confirmed visible self throw carries KNIFE and the aimed cell in the private display receipt, allowing the renderer to use the original knife image and spin. A replay or full snapshot does not animate it again.

## SYNC30

새 버전의 두 좌석만 같은 방에 참가한다. 각 좌석의 현재 소지품에서 wandchoice는 주입 후보, imbuelevel은 원본이 공개할 수 있는 강화값이다. 무기 충전, 성서, 매 명령, 급습과 가속은 각 소유자의 공개 상태에서 가져온다. 매가 관측한 지형은 소환자의 관측에만 포함한다. 로비에는 결투가와 성직자가 추가됐다. LAN v1 프레이밍을 유지한다.
