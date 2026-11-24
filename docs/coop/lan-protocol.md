# LAN 프로토콜 v1 및 로비

## 실행과 접속

libGDX 파일 서비스와 게임 버전이 초기화된 새 엔진의 소유 스레드에서 `LanRoom(seed)`를 만들고, 정기적으로 `pump()`를 호출한다. 방장이 종료하면 같은 스레드에서 `close()`한다. 기본 수신은 wildcard TCP 47821, UDP 47820이다. 주소와 포트를 지정하는 생성자에서 0을 사용하면 OS가 빈 포트를 고른다.

로컬 방장도 클라이언트 작업 스레드에서 loopback TCP를 열고 `room.hostJoinCommand()`로 참가한다. 방장 전용 임의 UUID는 프로세스 내부에서 전달한다. 검색·로비·로그에 싣지 않는다. 게스트는 `LanClient.discover(timeoutMs)` 결과의 주소 또는 직접 입력한 IP와 포트로 `new LanClient(address)`를 호출하고 `request("JOIN")`을 전송한다. 한 연결의 request는 직렬화된다. 메시지가 없는 동안에도 10초보다 짧은 간격으로 PING 또는 STATE를 요청해야 한다.

UDP 검색은 `SPD1 DISCOVER` 한 데이터그램을 전송한다. 열려 있는 방은 `SPD1_ROOM <roomUUID> <tcpPort>`로 응답한다. 원격 주소는 수신 데이터그램에서 구한다. 기본 검색 주소는 255.255.255.255이며 인터페이스별 브로드캐스트·유니캐스트 주소를 지정할 수 있다. 최대 30초·64개 방까지 UUID로 중복 제거한다. 방 잠금 후에는 응답을 끈다. UDP를 열 수 없으면 방장은 UDP_UNAVAILABLE을 조회할 수 있고 같은 TCP 포트가 계속 동작한다.

## TCP 프레임

4바이트 big-endian 길이 뒤에 1~2048바이트 ASCII 본문이 온다. 요청은 `1 requestID sessionID verb [args...]`, 응답은 `1 requestID result [args...]`이다. 첫 HELLO만 세션 위치에 하이픈을 사용한다. 요청 ID는 연결마다 1 이상의 증가하는 signed long이다. 같은 요청 ID 또는 작은 번호는 REPLAY다. 거부에도 번호가 소비된다. 같은 TCP 스트림 안의 분할·연속 프레임은 길이에 맞춰 읽는다. 요청은 ASCII 영숫자·하이픈·밑줄·공백과 최대 12개 토큰으로 제한한다.

|요청|성공 응답과 의미|
|---|---|
|HELLO|HELLO roomUUID. 클라이언트 생성자가 교환한다.|
|JOIN 또는 HOST localCapability|JOINED seat player. 호스트 0, 게스트 1. 서버가 연결 객체에 좌석을 묶는다.|
|SELECT WARRIOR 또는 SELECT HUNTRESS|OK. 직업을 바꾸면 해당 좌석의 준비가 풀린다.|
|READY 0 또는 READY 1|OK. 직업 선택 후 사용한다.|
|STATE|게임 전·중단 시 LOBBY state seat0 seat1. 빈 좌석은 하이픈, 참가 좌석은 player:class:ready다. 선택 전 class는 null이다.|
|START|방장만 두 좌석이 준비됐을 때 GAME_STARTED를 받는다. 시작·잠금·엔진 초기화는 한 실행 흐름에서 처리한다.|
|STATE (게임 중)|행동권자는 TURN turn actor ownCell ownHP와 선택 항목을 받는다. 나머지는 WAITING turn actor ownCell ownHP를 받는다. TURN의 F:kind:id는 발밑 더미, I:kind:id는 자신의 가방이다. kind는 FOOD 또는 WEAPON이다. ENEMY:cell과 FLOOR:cell은 현재 기본 시야 안 인접 셀만 나타낸다. 원본 엔진이 이동·공격 가능 여부를 최종 판정한다.|
|ACT player seat actor turn action cell itemHandle|실제 CoopSession.act 성공 후 APPLIED appliedTurn nextTurn ownCell. WAIT·MOVE·ATTACK은 handle 0, PICK_UP은 발밑 cell/더미 handle, EAT·EQUIP은 자신의 가방 handle을 쓴다. 목적지가 없는 행동에는 cell 0을 보낸다.|
|PING, DISCONNECT|각각 PONG, BYE. DISCONNECT는 응답을 소켓에 쓴 뒤 연결을 닫는다. TCP 종료만으로도 단절을 감지한다.|

클라이언트는 STATE로 새 로비·턴을 읽는다. v1은 요청 하나에 응답 하나이며 자발적인 월드 업데이트 스트림은 없다. 아이템 핸들은 활성 좌석에 제공한 해당 차례에서만 해석한다. 다른 사람이 차지한 인벤토리를 열람할 수 없다. 요청 큐 수신은 성공 응답을 생성하지 않는다.

## 상태와 실패

- LOBBY_OPEN에서 게스트가 들어오면 PLAYER_JOINED가 된다. 두 좌석 모두 준비하면 READY, 준비 해제·재선택 시 PLAYER_JOINED로 돌아간다. 게스트가 게임 시작 전 나가면 빈 좌석을 다시 연다.
- 방장의 START는 STARTING → ROSTER_LOCKED → IN_GAME 순서로 처리한다. 중간 두 상태는 같은 pump 호출 안에서 지나간다. 새 참가와 직업 변경을 막은 다음 실제 CoopSession.open/join을 호출한다.
- 엔진 예외·치명상 또는 진행 중 게스트 단절은 PAUSED로 이어진다. 잠금과 좌석을 유지하고 이후 ACT를 거부한다. 방장 단절·명시적 방 종료·I/O 워커 실패는 CLOSED다. 서버 내부 엔진 실패 사유는 소유 스레드에서 failure()로 조회한다.
- ERROR VERSION, SESSION, JOIN_FIRST, DUPLICATE, FULL, LOCKED, AUTH, CLASS, READY, NOT_READY, FORMAT, COMMAND, NUMBER, STATE, TURN, REPLAY, ACTION, ITEM, INVALID, ENGINE_STOPPED를 정의한다. 세션·좌석·행동권 검증 후에도 엔진이 잘못된 행동을 거부할 수 있다. 클라이언트 전송 예외 코드는 CONNECT_TIMEOUT, CONNECT_FAILED, WRITE_TIMEOUT, READ_TIMEOUT, CONNECTION_LOST, PROTOCOL이다.
- 프레이밍 손상·과대 길이·큐 포화·요청 빈도 초과는 해당 연결을 종료한다. 입력 큐 64개, 동시에 열린 TCP 연결 8개, 연결별 출력 16프레임, 초당 128요청을 제한한다. 한 pump에서 최대 32요청을 처리한다. 수신 워커는 nonblocking SocketChannel만 사용한다. 불완전 입력/유휴 연결은 마지막 완전한 프레임 후 10초, 출력 정체는 5초가 한도다. 단절 인지 전에 이미 적용한 행동은 롤백하지 않는다.

## 플랫폼 및 후속 작업

Android Manifest의 INTERNET과 ACCESS_NETWORK_STATE, minSdk 21, targetSdk 36을 유지한다. 방 검색은 UDP 데이터그램을 사용하며 주변 Wi-Fi AP 목록이나 SSID를 조회하지 않는다. AP 격리나 핫스팟 정책에 따라 브로드캐스트 또는 기기 간 TCP 자체가 차단될 수 있다. Android 16 로컬 네트워크 보호 opt-in 및 이후 target SDK의 권한 요구를 실기기 단계에서 확인한다. iPadOS 채택 시 로컬 네트워크 권한 설명과 브로드캐스트 사용 조건은 플랫폼 실행부에서 처리한다. [Android 로컬 네트워크 권한](https://developer.android.com/privacy-and-security/local-network-permission)

Phase 5는 v1 호환 또는 명시적 버전 거부를 유지하면서 호스트가 확정한 state version, 연결과 분리된 action ID, 결과 캐시를 추가한다. 행동 적용 후 응답을 잃은 경우와 엔진 중단 전 일부 효과가 발생한 경우를 별도 결과로 표현한다. 각 좌석의 관측 범위로 만든 초기 스냅샷과 증분 변경을 독립 JVM에서 대조한다. 저장과 재접속 식별 정보는 Phase 7 범위다.

## Phase 5 호환 모드

기존 v1 방과 ACT는 Phase 4 회귀 경로로 유지한다. 호스트가 생성 시 선택하는 SYNC1 방은 동일 TCP 프레이밍과 로비를 사용하며, 참가 전 MODE 협상과 별도의 DO·SYNC·PAGE를 요구한다. 두 모드의 행동 명령은 섞어 쓸 수 없다. [SYNC1 명세](state-sync.md)

## Phase 6

SYNC1의 DROP·UNEQUIP·REVIVE, 좌석별 기억 지형과 파티 전멸은 [상태 동기화 명세](state-sync.md)에 정의한다. v1 방은 신규 동사에 ERROR MODE를 반환한다. 두 방 모드 모두 일반 피해로 쓰러진 좌석을 행동 순서에서 제외하고 호스트의 아이템 귀속 검사를 적용한다.

## 영구 저장 SYNC1 방

호스트가 persistentGame을 선택하면 최초 좌석 등록에 ENROLL이 추가된다. 게임 명령에는 epoch가 붙고 단절 후 CHALLENGE, PROVE, SYNC, LOOKUP, CONFIRM을 사용한다. 기존 프레이밍·요청 빈도·페이지 크기 제한을 유지한다. [메시지 및 상태 전이 명세](reconnect.md)

## Phase 8 UI

앱 UI의 로컬 호스트 좌석과 게스트 좌석은 모두 기존 MODE SYNC1 및 ENROLL을 사용한다. 로비 STATE와 게임 SYNC는 UI 작업자에서 주기적으로 조회한다. LAN 기본 포트는 UDP 47820과 TCP 47821이다.

### SYNC2 starter staff

The TCP envelope is still v1. MODE SYNC2 negotiates mage support before joining. A SYNC1 seat can play in a warrior/huntress room. A mage roster requires both seats to negotiate SYNC2, including resumed connections. The logical action is ZAP with the selected enemy cell and the seat's current equipped staff handle. Aiming and cancelling are local. The host checks the original Ballistica path, direct observation, caster, charge, and target before consuming time. Owned STAFF rows append current charges and capacity. A successful caster-only receipt row contains action ID, ZAP, target cell, APPLIED. LOOKUP returns the ordinary durable outcome. Refresh and replay do not spend another charge.

### SYNC3 sewer travel

Negotiate MODE SYNC3 before joining. Both seats must support this mode for SEARCH, CONSENT, CANCEL_TRAVEL or TRAVEL. A multi-floor save requires SYNC3 for reconnect. The same DO epoch, authenticated seat, actor, Action ID, version, turn and durable receipt checks apply. SEARCH uses the actor's current cell and original intentional search cost. Approval and withdrawal are available only to the waiting seat. They commit public state without advancing actor time. The active seat can commit TRAVEL for the same staircase on the same turn. All ordinary actions and reconnect boundaries invalidate prior consent.

The floor row encodes branch,depth. A stair row is published only when both players can see and use that regular staircase. It encodes cell,agreement. The host resets public handles, pending transfers and delta bases on floor changes. The replica rejects a delta that changes floor and installs a complete validated snapshot. A generated destination with unsupported actors is declined as REJECT_FLOOR. The host rebuilds the original boundary, issues fresh handles and publishes the refusal with its receipt. Ordinary engine or disk faults continue to pause the session.

### SYNC4 하수도 방

표시 모델의 몬스터 종류에 Piranha와 Sentry를 추가했다. 이전 표시 모델은 새 종류를 해석할 수 없어 동기화 방은 MODE SYNC4를 요구하고 이전 MODE에 ERROR VERSION을 응답한다. 원본 v1 비동기화 시험용 방과 저장 형식 2는 유지한다. 직접 관측하는 감시탑이 충전 중이면 charge<cell>=1을 해당 좌석에만 보낸다. 표적 좌석과 준비 타이머는 전송하지 않는다.

### SYNC5 2층 유령

양쪽 좌석은 MODE SYNC5를 협상한다. SYNC4 이하는 동기화 방에서 ERROR VERSION을 받는다. 새 공개 몬스터 값은 Ghost와 FetidRat이다. 보이고 인접한 유령 칸에만 quest<cell> 행으로 OFFER, HUNT 또는 REWARD를 제공한다. stench<cell>은 직접 시야 안에 있는 악취 가스다. ACCEPT_GHOST, GHOST_WEAPON, GHOST_ARMOR에는 유령 칸과 아이템 핸들 0을 지정한다. 호스트가 시야·행동권·좌석·월드 플래그·안전한 수령을 재검증한다. 이미 발행한 보상은 같은 Action ID로 조회할 수 있다.

### SYNC6 꿀단지

이 빌드의 동기화 방은 MODE SYNC6를 요구한다. 공개 종류 Bee, HONEYPOT, SHELL을 추가했다. 자기 좌석이 소유한 꿀단지에 SHATTER와 현재 위치, 공개 아이템 핸들을 보낸다. 원본 비투척 깨뜨리기와 같이 독립적인 한 턴을 더 소비하지 않는다. 원본 벌 소환·조각 귀속과 행동 영수증을 같은 세대에서 확정한다. 가방의 조각을 내려놓거나 전리품을 줍는 행동은 기존 DROP 및 PICK_UP 검증을 거친다. 비공개 벌-조각 식별자는 참관자에게 전송하지 않는다.

## SYNC7 상자

두 좌석은 MODE SYNC7을 협상한다. 직접 보이는 상자는 h<cell>=CHEST 또는 CRYSTAL_CHEST로 전송한다. 위장한 미믹은 같은 행을 사용한다. OPEN은 인접 셀과 핸들 0을 사용하며, 마력탄은 상자에 조준할 수 있다. 위장이 해제된 다음 확정 경계에서만 몬스터 행을 공개한다.

SYNC8은 수정 미믹을 추가한다. 위장 중에는 CRYSTAL_CHEST만, 노출 후 직접 시야에 있는 동안에만 CrystalMimic 몬스터 행을 보낸다. 보관품·소유자 및 보이지 않는 도주 경로는 전송하지 않는다.
