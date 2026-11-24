# 엔진 분석 — v4.0.2

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
