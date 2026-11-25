# 엔진 분석 — v4.0.2

## 의뢰 상태와 소유자

Wandmaker.Quest가 생성한 두 원본 아이템과 진행 플래그를 원본 Bundle에 유지한다. 호스트 CoopSession이 좌석, 인접성, 의뢰물 소유자를 검사하고 SeatProjection이 해당 좌석의 공개 대화 상태를 만든다. 좌석 재접속에는 동일한 영수증과 스키마 5의 HEAD를 사용한다.

## 감옥 Actor와 층 저장

Guard와 DM100은 호스트의 원본 Mob AI, Ballistica, damage, occupyCell, Buff와 단일 Actor 시계를 사용한다. Guard의 끌려간 영웅이 자기 행동을 중단한다. 감옥 스프라이트와 물·타일은 좌석의 공개 상태에서 선택한다. 7층에 도달한 저장은 스키마 5로 여섯 휴면 층을 담는다.

## 이전 상점 체크포인트 · P1-A

SeatProjection이 관측 가능한 상품 가격과 자기 판매품만 내보낸다. CoopScene과 WndSeatTrade가 명시적 확인을 받고 기존 SeatActions 경로로 보낸다. CoopTrade는 실제 Hero와 Shopkeeper를 검사한 뒤 WndTradeItem의 원본 거래 메서드를 실행한다. 판매품은 상인의 원본 Bundle과 ItemOwnership의 물리적 위치 표에 함께 들어간다.

좌석별 UI 배치는 SeatTickets에 저장하고 권위 있는 아이템·영수증은 같은 호스트 저장 세대에 넣는다. SYNC14의 공개 행으로 현재 인벤토리와 퀵슬롯을 연결한다. 원본 PrisonLevel을 처음 방문한 방은 최대 다섯 휴면 층을 가진 스키마 4로 이어진다. [표시 경계](UI-ARCHITECTURE.md) · [상태 동기화](docs/coop/state-sync.md) · [저장 형식](docs/coop/save-format.md)

현재 협동 저장은 이후 원본 층 생성에 관여하는 안내서의 미발견 항목, 원본 다리 방의 배치 제한, 몬스터의 추격 경로와 경계 상태를 호스트 소유 데이터로 보존한다. 클라이언트가 사용하는 표시 행에는 직접 보이는 식물의 원본 타일 번호만 추가했다. [저장 명세](docs/coop/save-format.md) · [동기화 명세](docs/coop/state-sync.md)


## 하수도 층과 보스전

CoopFloors가 branch 0의 현재 층과 최대 네 개 휴면 층을 관리한다. CoopSession은 같은 두 Hero를 이동시키고 실제 Actor 스케줄러는 활성 층만 실행한다. CoopSaves는 이동·몬스터 전리품·상태 버전·영수증을 같은 세대로 발행한다. 수신 좌석의 SeatProjection에서 볼 수 있는 Goo 충전 단계만 표시한다. [현재 저장 구조](docs/coop/save-format.md) · [검증 경계](docs/coop/sewer-depths.md)

## 원본 효과와 협동 실행 경계

석상은 기본 무기의 통계·사정거리와 실제 Enchantment.proc를 조합한다. 기존 확정 경계에서 단일 Actor 스케줄러가 Pushing 및 다트의 지연 피해를 소진한다. 원본 Char.move·Level.occupyCell·Trap.trigger가 착지를 처리하고, 각 좌석의 직접 시야만 공개 지형·블롭 행으로 투영된다. 철 열쇠의 Notes 변경과 잠금 해제는 다음 행동 영수증을 포함한 단일 세대로 저장한다. [구현 및 시험표](docs/coop/unstable-effects.md)

기준 소스는 57a4e06a4caf162446d1c28caa7983f0493fecf0이다. 아래 메서드는 원본에서 확인했다. 협동 처리 설명은 후속 단계의 수정 설계다.

## 전수 검색 범위

추적 중인 모든 Java 소스에서 공백을 허용해 찾은 직접 표기 Dungeon.hero는 325개 파일에 2,159회 등장한다. 주석까지 포함한 어휘 검색이다. 파일별 모든 위치는 [dependencies.tsv](docs/coop/dependencies.tsv)에 기록했다. 같은 방식으로 층·재화·퀵슬롯, 시야 배열, 입력 상태, 통계 참조도 색인했다. Dungeon 내부의 비한정 hero, 인자로 전달된 별칭, 상속 메서드의 간접 호출은 직접 표기 수에 포함되지 않는다. 다음 표는 그중 실행 흐름에 중요한 간접 의존성도 포함한다.

|실행 경로|재사용할 원본 동작|협동 모드에서 필요한 경계|
|---|---|---|
|Dungeon.init → GamesInProgress.selectedClass.initHero|장비 초기화, 직업 특성, Hero.live|현재는 영웅 하나를 생성하고 퀵슬롯, 퀘스트, Statistics, Notes까지 초기화한다. 월드 초기화는 한 번만 하고 좌석마다 직업 초기화를 호출한다.|
|Hero.handle → Hero.act → actMove/actAttack/actPickUp/actTransition|HeroAction 분기 및 실제 행동 비용|handle은 로컬 셀 클릭의 의미를 추론한다. 호스트에서 현재 좌석과 관측 가능한 대상, 점유, 거리, 상태를 확인한 후 해당 Hero의 curAction을 실행한다.|
|Actor.init/process/spend/next/fixTime|최소 time을 고르고 동률은 높은 actPriority 우선. 이동 속도와 버프는 기존 시간 소비 사용|init은 영웅 하나와 몹·블롭을 등록하고 몹 표적을 복구한다. 영웅 모두를 먼저 등록한다. 같은 시간·우선순위의 영웅은 HashSet 순회에 의존하므로 협동에서 안정적 좌석 순서를 정의한다. process의 단일 영웅 생존 검사와 sprite.wait, GameScene.update의 notify를 분리한다.|
|Dungeon.saveAll → Actor.fixTime|정수 최소 시간을 모든 actor에서 차감하고 Statistics.duration을 올리는 리베이스|Actor.now는 줄어들 수 있다. Turn ID와 State Version은 별도 단조 카운터로 저장한다. 공통 몹·버프·블롭은 구역 수에 관계없이 한 번만 등록한다.|
|Char.attack/damage, Hero.damage/die/reallyDie/resurrect|명중, 저항, 방어, 흡수, 직업별 효과|실제 타격 전 HP를 피해 사건에 보관한다. Char.damage의 HP 차감 및 추가 피해와 Char.attack의 강제 HP=0 경로도 검토한다. Hero.die의 앙크 처리와 reallyDie의 전역 게임 종료를 좌석별 상태와 파티 종료로 분리한다.|
|Mob.act/chooseEnemy/destroy/die, Goo|FOV, 접근성·거리 비교, 상태 전이, 기존 보스 패턴|chooseEnemy가 Dungeon.hero만 후보에 넣거나 동거리에서 우선한다. 유효한 영웅을 열거한다. destroy의 EXP·장비 효과 및 die의 전리품에 행위자를 전달한다. Goo의 enemy 비교와 LockedFloor 버프도 대상별로 검사한다.|
|Belongings(Hero), Item.execute/doPickUp/doDrop/collect, Hero.actPickUp|가방과 장비는 이미 owner를 보유하고 복원 시 장비를 owner에게 활성화한다.|Item.curUser/curItem은 static이며 나중에 실행되는 콜백이 읽는다. 요청마다 소유자와 아이템을 고정한다. 호스트에서 heap 제거와 가방 반영을 같은 행동으로 커밋한다. 합쳐지는 스택과 바닥의 소유권도 보존한다.|
|Buff.attachTo/act, Actor.add|대상 Char와 actor 기반 지속시간|대상과 시전자를 구분하고 레벨 봉쇄·시간정지처럼 Dungeon.hero 버프를 찾는 호출을 범위별로 검토한다.|
|Hero.act → Level.updateFieldOfView → ShadowCaster.castShadow, Dungeon.observe|벽 차폐, 기존 시야 거리, 특수 감지 규칙|Hero.act가 level.heroFOV를 참조하고 observe는 level.visited에 OR한다. 재사용할 shadow 계산과 c == Dungeon.hero 분기를 분리한다. 좌석·층별 visible/visited/mapped와 마지막 관측 지형을 저장한다. heap.seen, 정신 감지, 동료 시야도 수신자 기준으로 처리한다.|
|GameScene.create/update/ready, Toolbar|픽셀 UI, 효과, 입력과 카메라|단일 영웅 sprite, 퀵슬롯, 행동 가능 표시를 로컬 좌석에 연결한다. 시야 밖 위치는 호스트에서 제거한다. 숨은 몹이나 구역 경계는 초기·재접속 스냅샷에도 새지 않아야 한다.|
|Level.activateTransition → InterlevelScene.descend/ascend → Dungeon.switchLevel|층 생성, 기존 층 로드, 출입구 선택, 충돌 회피|별도 scene 스레드가 저장과 전환을 수행한다. 파티 동의 정책 확정 후 호스트 큐에서 한 번 전환하고 안전한 서로 다른 셀에 배치한다.|
|Dungeon.saveGame/saveLevel/saveAll/loadGame/loadLevel, Level.storeInBundle/restoreFromBundle|기존 Bundle 형식, actor ID와 장비 직렬화|saveAll은 게임과 층을 차례로 쓴다. FileUtils의 파일별 .spdtmp로는 두 파일의 세대 일치가 보장되지 않는다. 협동 전용 세대 디렉터리에 모두 쓰고 검증 후 현재 세대 포인터를 교체한다. 저장 직전 행동 완료 경계, 좌석·행동 중복 기록·탐험·RNG 상태를 함께 복원한다.|

## 실행 및 정보 경계

공통 Java 엔진의 단일 실행 흐름이 월드, RNG, Actor 등록과 시간을 소유한다. TCP 읽기 스레드는 크기·버전·빈도·값 범위를 확인한 요청만 큐에 넣는다. 적용 시점에 session/seat/player/actor 매핑, turn, state version과 action ID를 다시 검사한다. 재전송 시 이미 확정한 결과를 반환한다. 승인 후 수신자별 상태 변경과 관측 상실을 전송한다. 거부나 끊김은 새 행동 시간을 소비하지 않는다. UDP 발견 실패 시 직접 IP 연결을 사용한다. 재접속은 잠긴 로스터에서 기존 좌석만 복원하며 비밀값을 로그에서 제외한다.

전투 구역은 통행 가능한 지형과 교전 관계에서 산출하는 표시 데이터다. 합치거나 나눌 때 Actor.process나 피해 계산을 호출하지 않는다. 월드 하나에 호스트와 클라이언트를 함께 띄우면 전역 상태가 공유되므로 네트워크 검증은 별도 JVM에서 한다.

## Headless 제약

:core는 Java 라이브러리로 :SPD-classes와 :services에 의존한다. Noosa Game.create/render는 그래픽·입력·GL을 사용하고 FileUtils는 Gdx.files에 의존한다. Hero.ready는 sprite.looping()을 호출하며 공격과 이동 완료 콜백이 턴을 재개한다. HeadlessApplication만 띄워서는 이 경로가 실행되지 않는다. Phase 2에서 저장 루트와 프레젠테이션 완료 신호를 분리하고 동일한 게임 메서드를 구동한다. Random.resetGenerators는 시드 없는 기본 생성기를 만든다. Dungeon.seed 하나로 모든 전투 결과를 재현할 수 있다고 가정하지 않는다.

## Phase 3의 오프라인 실행 경로

`CoopSession.open(seed, playerId, type)`은 libGDX 파일 서비스가 준비되고 던전이 로드되지 않은 엔진에서 호출한다. 세션당 실제 Level은 하나다. `Dungeon.init(firstClass)`, `newLevel`, `switchLevel`은 호스트가 한 번 호출한다. `join`은 빈 입구 인접 셀을 찾아 별도 Hero, Belongings, 시작 장비와 버프를 만들고 `Actor.add`로 등록한다. 게스트 장비 생성 중에는 잠시 별도 RNG를 사용해 월드 RNG의 다음 추출값을 보존한다. 지원하는 두 직업은 어느 좌석에도 들어갈 수 있다. 같은 프로세스에 두 월드를 띄우는 API는 제공하지 않는다.

|참조의 역할|협동 경로에서 상태를 얻는 방법|원본 코드에서 추적한 예|
|---|---|---|
|행동 주체|세션의 현재 좌석 → Hero. 행동 비용은 해당 Actor에 부과|`Hero.handle/act/rest/onAttackComplete`, 전투 후 `Invisibility.dispel(this)`|
|아이템 소유자|행동자의 Belongings와 Backpack.owner를 검사한 뒤 실제 아이템을 전달|`HeroClass.initHero → Item.collect(Bag)`, `Food.execute/eatingTime`, `KindOfWeapon.doEquip`, `Armor.affixSeal`|
|피해·효과 대상|실제 Char 인스턴스와 Buff.target을 전달|`Char.attack/damage`, `Mob.chooseEnemy/surprisedBy/die`, `Regeneration.act`, `WarriorShield.act`|
|로컬 UI|협동 호스트는 화면이 없다. 원본 입력 준비와 애니메이션 완료 경계만 사용|`Hero.checkVisibleMobs`의 퀵슬롯 자동 조준, `Item.curUser/curItem`의 비동기 선택, `GameScene` 입력은 로컬 UI 경로|
|관찰자|기본 시야는 실제 영웅과 그 영웅의 배열로 계산|`Level.updateFieldOfView(c, fieldOfView)`. 호스트의 기존 heroFOV는 게스트의 배열과 분리된다. 탐험·지도·특수 감지 공유 문제는 Phase 6에 남아 있다.|
|월드|현재 던전이 소유한 단일 지도·몹·스케줄러·통계 사용|`Level.mobs`, `Actor.all/add/findById`, 전역 발견 통계·색상/라벨. 두 번째 좌석이 월드를 초기화하지 않는다.|
|싱글플레이 전용|기존 진입점과 저장 형식 유지. 협동에서 기존 저장·복원·삭제 요청은 거부|`Dungeon.saveAll/loadGame/deleteGame`, `Rankings`, 부활·층 전환 화면|

`docs/coop/hero-roles.tsv`는 v4.0.2의 직접 참조 2,159개(325개 파일)를 위 일곱 역할로 1차 분류한 원본 줄 번호표다. `python3 scripts/classify-hero-references.py`로 재생성한다. 같은 줄의 두 참조도 각각 센다. 표의 모든 행은 어휘 기반이며 물음표는 주변 구문만으로 판별하기 어려워 파일 영역에서 유추한 역할이다. 실제로 검토하고 실행한 경로는 위 표와 TEST_REPORT의 사례에 한정한다. 별칭인 curUser, 선택된 가방, buff.target, Mob.enemy, quickslot.lastTarget, heroFOV 및 스프라이트 콜백은 직접 참조 색인 밖에서도 추적했다.

### 턴 처리와 출력

1. 세션을 만든 스레드가 `Actor.process`를 실행한다. 최솟값 time, 원본 actPriority 순서를 보존하고 이 둘이 같은 영웅끼리만 좌석 순서로 결정한다. 입력이 필요하면 현재 Hero가 준비 처리를 하고 호출자에게 돌아온다. 몬스터·버프는 동일한 원본 스케줄러에서 실행된다.
2. `act(playerId, offeredTurn, action, cell, item)`은 현재 좌석과 입력 차례를 검사한다. 이동은 한 칸의 점유·지형, 공격은 시야 안의 인접 적, 획득은 발밑 더미의 맨 위 객체, 장비·식량은 가방의 소유권을 확인한다. 거부된 오래된 요청은 시간과 좌석을 바꾸지 않는다. 승인된 입력 하나를 처리한 뒤 다음 입력 차례까지 진행한다.
3. `EngineSprite`가 이동·공격·조작 완료 콜백을 같은 스레드에서 호출한다. 명중·피해, 장비·식량 소모, 이동 비용과 몬스터 AI는 원본 코드를 사용한다. 지원 범위를 벗어난 몬스터가 행동할 차례거나 엔진 실행에 실패하면 세션을 중단하고 사유를 노출한다. 이미 승인된 행동의 효과가 발생한 뒤 중단될 수 있어 같은 행동을 재시도하지 않는다.
4. `turn()`은 입력 차례마다 증가한다. 원본 `Actor.now()/fixTime()`의 리베이스와 독립적이다. 세션의 Hero/Item 객체는 엔진 내부에서만 사용한다. Phase 4의 네트워크 스레드는 이 참조를 받지 않는다.

## Phase 4 LAN 호스트

`LanRoom`을 연 스레드가 요청 큐를 비우고 로비와 CoopSession을 소유한다. `LanTransport`의 I/O 스레드는 UDP 검색과 nonblocking TCP 연결·프레이밍·유량 제한을 처리한다. 검증된 프레임을 제한된 큐에 전달하며 게임 객체를 참조하지 않는다. `LanClient`는 IP 직접 연결, 방 검색과 직렬 요청/응답을 제공한다. 로컬 호스트 좌석도 loopback과 방장 전용 자격값을 통해 같은 ACT 검증을 받는다. 게임 상태 공유와 시간 계산은 기존 CoopSession과 Actor가 담당한다.

호스트는 요청 연결·세션·player·seat·actor·turn을 대조하고, 해당 턴에 공개한 아이템 핸들을 원본 Item으로 해석한 뒤 행동을 실행한다. 클라이언트는 자신의 위치·HP·행동권과 현재 볼 수 있는 인접 셀, 허용된 아이템 선택 항목만 요청해 읽는다. 정확한 와이어 형식, 상태 전이, 오류 코드, 시간·크기 제한은 [LAN 프로토콜](docs/coop/lan-protocol.md)에 있다.

## Phase 5 확정 경계와 공개 상태

LanRoom.synchronizedGame이 만든 방은 START에서 RoomSync를 엔진 소유 스레드에 연결한다. 인증된 DO마다 RoomSync.action은 좌석별 최고 ID와 결과 기록을 확인하고 원본 CoopSession.act를 최대 한 번 호출한다. 원본 스케줄러가 다음 입력 경계에 도달하면 두 좌석의 SeatProjection을 갱신한 뒤 새 버전을 공개한다. 예외에는 부분 적용 가능성을 남긴 종결 영수증을 저장하고 세션을 일시정지한다.

Level.updateDirectSight는 원본 시야 계산의 차폐·거리 부분이다. 기존 updateFieldOfView는 이 계산 뒤 원본의 특수 감지와 탐험 처리를 계속한다. 협동 전송은 매 확정 경계에서 각 영웅의 현재 위치로 직접 시야를 새로 계산한다. 전송 대상에는 자기 캐릭터·공개 장비/수량·버프, 현재 볼 수 있는 지형과 몹만 들어간다. Mob.wasDefeatedBy와 전후 관측을 확인한 직접 처치는 관측 상실과 구분한다.

StateImage는 좌석별 불변 공개 행과 정규화 해시를 갖는다. 호스트는 현재·직전 이미지를 비교해 변화한 행만 만든다. 클라이언트 DisplayReplica는 후보 상태의 해시를 직접 검증한다. SeatActions는 표시 모델을 교체해도 유지하는 논리 행동 번호와 재전송용 Pending을 관리한다. [프로토콜·공개 필드·자원 상한](docs/coop/state-sync.md)

## Phase 6 협동 규칙의 실행 경계

| 경로 | 보관 상태와 실행 책임 |
|---|---|
| CoopSession.knowledge / observe, FloorKnowledge.observe / reveal | 호스트가 branch·depth별로 좌석 수만큼 독립된 메모리를 보유한다. Level.updateDirectSight와 원본 ShadowCaster로 직접 보이는 discoverable 셀을 구해 visited와 마지막 관측 지형을 갱신한다. 자기 칸도 생존 중에만 보인다. 지도 효과로 배운 셀은 mapped에 따로 표시한다. |
| Hero.handle / getCloser / search, Foresight.attachTo | 협동 탐색 대상과 함정의 기지 여부는 행동자의 기억을 사용한다. 원본 Foresight 버프의 탐색 반경, 비밀 탐지와 셀 공개 경로를 실행해 해당 좌석에 기록한다. 기본 렌더링과 기존 원본 코드에는 Dungeon.hero와 level.heroFOV가 남는다. 좌석 공개 모델은 직접 시야를 다시 계산하며 level.visited·mapped·heap.seen을 수신자의 기록으로 가져오지 않는다. |
| CombatZones.calculate / corridor / edgeMask | 현재 HUNTING 상태인 몹의 실제 표적과 최근 두 입력 차례 안의 실제 공격 대상을 연결한다. 살아 있는 지원 몹과 영웅이 시야 안에 있고 통행 가능한 동서남북 경로가 8보 이내일 때 교전 영역을 만든다. 두 끝까지의 거리 합이 최단경로 길이+2 이하인 통행 셀을 취한다. 겹치는 영역을 합집합으로 묶는다. 같은 몹이나 영웅을 공유하면 그 셀에서 겹친다. 매 계산에서 죽음·차폐·표적 변경·관계 만료를 새로 평가한다. |
| SeatProjection.capture / writeMemory | 수신자별로 보이는 참여자와 지형만으로 경계를 재계산한다. 공개 경계 계산에 사용하는 차폐 지도에서 미관측 셀은 불투명하다. 공개된 셀에서 4방향 테두리만 전송한다. 몬스터와 아이템은 현재 시야에 한정하며 숨은 더미를 제외한다. |
| ItemOwnership, Heap.drop, Item.collect / merge / split / dropCoopUnit, CoopSession.act | 호스트의 세션 내 아이템 번호와 소유 player ID를 실제 Item에 대응시킨다. 바닥 최초 등록과 스택 분할 때 내부 번호를 부여한다. 시작 장비를 귀속하고, 가방 수용 가능 여부를 검사한 뒤 원본 추가·병합 경로에서 신규 전리품을 귀속한다. 분할한 한 단위는 새 번호와 기존 소유자를 가진다. 소비·병합으로 사라진 객체는 현재 가방·장비·더미에서 찾을 수 없고 공개 핸들도 회수된다. 강한 참조가 사라진 항목은 레지스트리의 약한 키에서 해제할 수 있다. |
| Char.damage / attack, Hero.die, CoopSession.fallen / revive / finishEffects, Actor.freezeCoop / process / fixTime | 방어·보호막 계산 뒤 HP 차감 직전 값을 보관한다. 일반 피해, 원본 추가 치명타와 직접 처형 분기를 구분한다. 양의 HP에서 강제 die가 오면 그 HP를 사용한다. 선행 HP가 없는 외부 0 대입 경로는 자원 소모 없이 최종 사망으로 처리한다. 생존자에게만 입력을 제안한다. 전투 불능인 영웅은 셀·Actor ID·장비를 유지하며 자신과 부착 버프의 남은 시간을 동결한다. Actor 등록을 유지한 채 선택과 시간 리베이스에서 건너뛴다. 부활 시 호스트의 현재 시각에 남은 시간을 더해 같은 Actor를 재개한다. |
| LanRoom.dispatch, RoomSync.apply, SeatActions.prepare, StateImage.rememberedTerrain / mappedOnly | 새 행동도 세션·좌석·턴·버전·영수증 캐시를 거친다. 부활 확정은 원본 스케줄러가 다음 입력에 도달한 뒤 두 좌석의 생존·HP·소지품을 한 버전으로 반영한다. 파티 전멸 결과도 캐시하여 터미널 상태에서 같은 영수증을 조회할 수 있다. 표시 코드는 DisplayReplica.image()에서 검증이 끝난 불변 모델을 받는다. |

전투 구역 계산은 로컬 배열과 BitSet을 사용한다. 실제 Actor 목록이나 PathFinder의 공용 거리 배열, RNG, HP, 행동 시간을 변경하지 않는다. 구역·현재 시야·공개 경계·전송용 아이템 핸들은 복원 후 재계산할 수 있다. 좌석별 visited·mapped·기억 지형, 내부 아이템 ID·소유자, 생존 상태·사망 직전 HP·동결된 상대 시각과 재전송 방지 기록은 저장해야 한다.

## Phase 7 저장 가능한 방

LanRoom.persistentGame은 하나의 엔진 실행 스레드에서 두 Hero의 원본 Bundle, Level, Actor 스케줄, 좌석별 탐험, 귀속 아이템, 난수 스택과 RoomSync 영수증을 하나의 체크포인트로 만든다. CoopSaves가 세대를 디스크에 발행한 뒤 TCP 응답을 허용한다. 엔진 실행 또는 저장이 불확실하면 정상 진행을 중지한다.

호스트 재시작은 LanRoom.resume가 담당한다. 두 좌석의 공개 키를 복원하고 네트워크 연결과 공개 모델을 새로 만든다. 각 좌석이 자신의 전체 모델을 검증할 때까지 입력을 잠근다. 기존 v1과 메모리 전용 SYNC1 진입점은 이전 동작을 유지한다.

[저장 형식](docs/coop/save-format.md) · [재접속과 Phase 8 API](docs/coop/reconnect.md)

## Phase 8 협동 알파

TitleScene 및 WelcomeScene에서 CoopScene으로 진입한다. HostedRoom의 단일 실행자가 저장과 Actor를 소유한다. 메뉴와 지도는 TCP 좌석 클라이언트가 게시하는 불변 Frame을 쓴다. 렌더링·입력·자격 보관 및 진단 경계는 [UI 아키텍처](UI-ARCHITECTURE.md)에 있다.

## 마법사 및 하수도 층 교체

WandOfMagicMissile.cast는 실제 장착자의 Hero와 원본 Ballistica를 받는다. CoopSession.act가 귀속·직접 시야·탄착 셀·적 생존·행동권을 검사한다. 원본 Wand의 피해·충전·턴 소비를 사용하며 장비 내부 Charger는 주인 Actor에 같은 ID와 시각으로 복원한다.

CoopFloors는 원본 Level 하나를 활성화하고 반대쪽 하수도 층 하나를 Bundle로 보관한다. 휴면 층은 스케줄러에 등록하지 않는다. RoomSync가 대기 좌석의 동의와 행동권자의 최종 이동을 각각 인증·확정한다. 새 층은 원본 Dungeon.newLevel로 생성하고, 재방문은 이전 Level과 바닥 귀속·관측 기억을 읽는다. 도착하면 두 캐릭터의 실제 Actor와 새 층의 몬스터·버프·스포너를 등록한다. 미지원 목적지 검출 때만 이동 직전의 정상 이미지로 복원한다. 다른 종류의 엔진 실패와 저장 실패는 일시정지한다. 저장 형식과 오류 경계는 docs/coop/save-format.md에 기록했다.

## 하수도 특수 방 체크포인트

CoopSession.basicMob은 원본 Mob 클래스를 기준으로 지원을 확인한다. Sentry.actCoop은 하나의 Actor 시계에서 노출된 표적 하나를 유지한다. Piranha.surprisedBy, Bee의 방어 AI, FetidRat의 경로 선택은 해당 Hero를 사용한다. Ghost.Quest.accept/claim은 엔진 소유 스레드에서 퀘스트 플래그와 실제 Item을 변경한다. RoomSync가 기존 영수증과 함께 저장한다. 벌과 조각은 ShatteredPot의 coopBee로 연결되며 휴면 층 재활성화 시 실제 바닥과 가방을 대조한다. SeatProjection이 관측된 충전·악취·인접 NPC 대화만 공개하고, CoopBoard와 CoopScene이 SYNC6로 입력을 전송한다. [세부 경로](docs/coop/sewer-region.md)

## 하수도 특수 Actor와 원본 아이템

CoopFloors.requireSupportedResidents는 최초 층과 이동 목적지에서 같은 허용 조건을 검사한다. CoopSession.floorItems는 미믹 보관품 및 석상 무기까지 물리적 위치를 열거한다. 스택을 분리하면 새 ID가 원소유자를 계승하고, 다시 합쳐지면 합쳐진 단위의 ID가 끝난다. RoomSync의 내구성 경계에서 원본 피해·아이템·영수증이 함께 저장된다. [저장 명세](docs/coop/save-format.md) · [공개 모델](docs/coop/state-sync.md)
