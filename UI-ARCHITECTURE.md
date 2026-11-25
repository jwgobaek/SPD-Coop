# 협동 화면과 실행 스레드

## 광산·도시 체크포인트

SYNC22의 공개 branch와 depth 쌍으로 보드의 수명을 관리한다. 광산 출구에는 재입장 제한을 표시하고 대장장이 장비 선택은 현재 좌석의 공개 소지품 핸들을 사용한다. [실행 기록](docs/coop/mines-city-results.md)

## SYNC21 동굴 표시

기존 CoopBoard와 공용 원본 타일맵이 보스방의 동굴 장식 인덱스를 그린다. 현재 시야에서 승인된 Actor의 과충전 상태와 환경 셀만 파티클·스프라이트 입력에 사용한다. 대장장이 대화의 확정 버튼은 좌석의 행동 요청을 보내고, 로컬 취소는 Actor를 실행하지 않는다. [시드·원본 전투·저장 검증](docs/coop/caves-dm300-results.md)

## SYNC20 감옥 보스방

동일한 깊이에서 원본 방이 바뀌면 타일·벽·안개·표시 객체와 좌석별 탐험 기억을 교체한다. 관찰한 원본 출구의 바닥·벽 프레임, 현재 보이는 함정 예고·화염·전격을 그린다. 원격 화면은 전투 Actor나 월드 RNG를 실행하지 않는다. 11층에서는 원본 동굴 타일·수면을 사용한다.

## SYNC19 공개 표시

CoopBoard는 수신자에게 보이는 Necromancer, SpectralNecromancer, NecroSkeleton, Bat을 원본 스프라이트로 표시한다. 준비 표시는 시전자 셀에만 묶인다. 서버 행동과 분리한 표시 메서드는 원본 소환 콜백을 호출하지 않는다. 충격파와 화염 방사는 같은 퀵슬롯에서 원본 이미지로 구분하고 빈 가시 셀도 조준할 수 있다.

## 휴대 지팡이 표시

SYNC18의 WAND 행은 소유자의 현재 핸들과 충전치를 가진다. 기존 CoopScene 조준과 아이템 선택 창을 사용한다.

## 장인 대화와 보상 선택

공개 상태에 인접하고 보이는 장인이 있을 때 대화창을 연다. 의뢰물을 가진 좌석에만 실제 두 보상 이름을 보낸다. WndOptions의 선택은 CoopController와 호스트의 ACTION 검사를 거친다. 닫기는 게임 상태를 변경하지 않는다.

## 감옥 거래 확인

WndSeatTrade는 실제 확정 클릭 시 좌석·세션·epoch, 현재 행동권, 공개 상품과 견적을 다시 확인한다. 새 스냅샷을 기다리던 중 시세나 상품·금액이 바뀌면 오래된 견적을 실행하지 않는다. 감옥 7층에서 CoopBoard는 좌석 카메라와 감옥 물·벽·안개 레이어를 유지한다.

## 이전 상점 체크포인트 · P1-A

6층에서 상품 칸을 누르면 해당 좌석의 가격과 잔액을 조회한다. 상품 칸에 올라선 뒤 구매를 확인한다. 상인 인접 시 자기 가방의 판매 및 상인 보관 품목의 환매 창을 이용한다. 창을 닫으면 턴, 골드, 행동 ID가 바뀌지 않는다.

장비를 선택하는 알려진 주문서는 가방과 퀵슬롯 양쪽에서 CoopScene.useItem을 거쳐 같은 WndSeatInventory 선택 창을 연다. 취소하면 네트워크 명령을 보내지 않는다. 원본 장비 선택 이후에만 현재 공개 버전과 소유권을 재검증한다.

## V4–V5 좌석 조작

CoopHud가 변경 불가능한 SeatQuickslots와 PublicDungeon을 읽는다. WndSeatInventory는 같은 원본 격자로 아이템 사용, 퀵슬롯 등록 및 주문서 대상 선택을 처리한다. 가방 탭과 순서는 자기 좌석의 실제 Belongings와 하위 Bag에서 온다. 소유·시야 확인을 마친 공격 좌표로만 MissileSprite를 그리며, 시각 효과 완료 콜백에는 엔진 명령이 없다.

현재 버전을 유지하는 동안만 선택 창의 입력을 승인한다. 상태 버전이나 층이 바뀌면 조준을 종료한다. 휴식은 연속 20회 상한과 단일 미확정 요청을 적용하고 승인된 다음 자기 차례에서만 계속된다. 동료 차례, 가시 적, 피해, 새 불리한 상태, 화면 전환 또는 연결 중단에서 멈춘다. 카메라 드래그와 메뉴의 배율·중심 복귀는 현재 기기의 표시 설정이다.

## 기본 던전 화면의 렌더 경로

호스트의 SeatProjection이 좌석별 공개 행을 생성한다. DisplayReplica가 검증한 StateImage를 PublicDungeon이 읽고, CoopBoard가 DungeonRenderMap의 공개 배열을 네이티브 타일맵과 FogOfWar에 전달한다. 층과 좌석이 같으면 보드·카메라·HUD·입력 리스너가 살아 있다. 지형 또는 관측이 달라지면 이웃 셀을 포함한 사각형을 갱신한다. 몬스터는 공개 Actor ID로, 바닥 아이템은 공개 핸들로 표시 객체를 유지한다. 지팡이 등 효과는 기존의 확정 이벤트에서만 시작한다.

CoopHud는 StatusPane의 배경, 원본 게이지와 초상화, Toolbar의 버튼 구현을 사용한다. WndSeatInventory는 WndBag의 그리드 계산, WndTabbed, InventorySlot의 실제 표시 구성 요소를 사용한다. 빠른 사용, 아이템 정보창, 지도 입력은 그린 StateImage를 CoopController에 전달한다. WndCoopBag은 옛 구현의 기록이며 일반 가방 버튼은 WndSeatInventory를 연다. [레이어별 책임과 비교](GRAPHICS-PARITY.md)

협동 보드의 식물은 SeatProjection이 수신 좌석의 직접 시야로 제한한 `plant<cell>` 행에서 읽는다. PublicDungeon과 CoopBoard가 원본 terrain features atlas의 식물 이미지를 표시하며, 시야를 잃거나 식물이 시들면 그 이미지를 지운다. 두 언어의 Sungrass 화면은 [검증 ZIP](docs/coop/sewer-plants-evidence.zip)에 있다.


## 하수도 효과 표시

CoopBoard는 검증된 공개 상태에서 원본 함정 아틀라스, 보이는 블롭의 색, 공개된 석상·황금 미믹·양의 스프라이트를 그린다. PublicDungeon은 인접 잠긴 문을 UNLOCK으로, 양과의 상호작용을 GREET로 변환한다. 지연 피해와 밀치기의 게임 시간은 호스트 Actor가 소유한다.

## 진입과 로비

`TitleScene`의 싱글플레이 버튼은 원래의 슬롯 조회와 `HeroSelectScene` 또는 `StartScene` 전환을 유지한다. 최초 설치의 `WelcomeScene`에서도 협동 모드에 들어갈 수 있다. `CoopScene`은 Noosa의 카메라, 원본 폰트, Chrome, 직업 스프라이트, 버튼과 대화창을 사용한다. 영어 및 한국어 문자열은 기존 번역 번들에 있다.

|실행 위치|소유 상태 및 API|
|---|---|
|렌더 스레드|CoopScene, CoopBoard, WndCoopBag. 컨트롤러에 명령을 제출하고 마지막 불변 Frame을 읽는다. 열린 Window가 있는 동안 아래 지도 입력 리스너의 재등록을 보류한다.|
|클라이언트 작업자|CoopController.host, discover, join, select, ready, start, saved, resume, reconnect, act, leave. 소켓 요청, 서명, 자격 파일 쓰기, 스냅샷 확인, 공개 모델 구성을 순서대로 실행한다.|
|호스트 엔진 작업자|HostedRoom이 한 스레드에서 LanRoom.persistentGame/resume/pump/saveAndClose를 호출한다. 단계 7의 세대 저장과 영수증 발행 순서를 유지한다. 엔진에서 직접 발생하는 음향을 이 스레드에서 차단한다.|
|LAN I/O|기존 LanTransport가 제한된 큐로 수신 명령을 전달한다. 로컬 방장도 TCP로 좌석을 등록한다.|

CoopController는 사용자 명령 하나만 진행시키고, 160 ms 간격의 작업자 폴링으로 로비와 턴을 갱신한다. 네트워크 지연 중에도 렌더 스레드는 프레임을 그릴 수 있다. 새 표시 모델은 DisplayReplica가 검증한 StateImage로 만든다. 호스트 화면도 같은 경로를 사용한다. 세계용 RNG 스택은 엔진 소유 스레드에 귀속한다. 화면 재구성은 새로운 입력권이나 Actor를 만들지 않는다.

## 표시와 입력

`PublicDungeon`은 좌석의 위치, HP, 경험치, 직접 보이는 몬스터, 전리품, 장비와 버프를 해석한다. 지도 중앙은 자신의 위치다. 현재 지형, 탐험 기억, 지도 효과 지형은 각각 다른 밝기·색을 사용한다. 보이는 z셀의 방향 비트로 얇은 전투 경계를 그린다. 타일 시트와 캐릭터 필름은 원본 리소스다. 인접 셀을 누르면 검증된 상태를 기준으로 이동·근접 공격·제자리 줍기·부활 중 한 가지를 제안한다.

가방은 자신의 공개 아이템 핸들만 갖는다. 서버가 장비, 소유권, 행동권, 셀을 다시 확인한다. 입력 시점의 이미지가 오래되면 명령을 제출하지 않는다. 한 논리 행동의 ID는 첫 전송 전에 좌석 파일에 기록한다. UI는 이동을 예측하지 않는다. 음향 및 동작 애니메이션은 연속된 확정 변경분과 관측 가능한 피해·사망을 근거로 재생한다. 상태 재조회 또는 새 전체 스냅샷은 과거 효과를 재생하지 않는다. 복잡한 무기 연출, 모든 몬스터 공격 애니메이션, 여러 칸 자동이동은 후속 범위다.

## 저장, 중단과 오류

호스트는 앱 전용 디렉터리의 CoopSaves 세대 파일을 사용한다. 별도의 coop-seats-v1 디렉터리가 서명 자격과 마지막 미확정 Action ID를 보존한다. Android API 23 이상은 AndroidKeyStore의 비내보내기 P-256 개인 키를 참조한다. API 21~22는 no-backup 앱 디렉터리에 소프트웨어 키를 저장한다. Desktop 소프트웨어 키는 게임의 사용자 디렉터리에 저장한다. 기기 데이터 삭제·키 분실 시 대리 좌석 등록을 허용하지 않는다. LAN 전송은 평문이다.

백업을 선택하면 진행 되돌림 확인을 받는다. 새 epoch에 재접속하는 좌석은 다시 동의해야 한다. 인증 후 전체 공개 스냅샷을 교체하고 미확정 영수증을 같은 논리 ID로 조회한다. 앱 일시중지 또는 종료 요청은 작업자에게 저장 및 연결 정리를 위임한다. 프로세스 강제 종료는 이미 발행한 마지막 세대까지 복구한다. OS가 백그라운드 호스트 실행을 보장할 필요가 없다.

진단 내보내기는 사전에 정한 필드만 텍스트로 만든다. 빌드 리비전, 프로토콜·저장 형식, 자신의 세션·좌석·턴·버전, 세션 상태, 전송량과 오류 유형을 제공한다. 예외 보고에는 예외 유형과 게임 호출 지점 하나를 남긴다. Android 공유 시트는 사용자가 내보내기를 눌렀을 때 열린다. Desktop은 클립보드를 사용한다. 지원 범위 및 검증 결과는 TEST_REPORT.md를 참조한다.


## 수정 지점과 검증 경계

|경로|주요 클래스·메서드|
|---|---|
|게임 진입·수명|TitleScene.create, WelcomeScene.create, CoopScene.create/update/onPause, ShatteredPixelDungeon.destroy, GameScene.releaseInactiveDungeon|
|입력과 표시|CoopScene.game, CoopBoard, WndCoopBag, PublicDungeon.tap/use/actions, PointerEvent.hasActivePress, SeatProjection.capture, StateImage.snapshotBytes|
|확정 턴·통신|CoopController.act/poll, HostedRoom.close, DisplayReplica, LanClient.transferredBytes, LanRoom, CoopSession.basicFood, Door.enter/leave|
|저장·재접속·플랫폼|CoopSaves.inspect, SeatTickets, SeatCredential, CoopPlatform, AndroidSeatKeys, AndroidLauncher, CoopDiagnostics.report, CoopPlatform.export|
|회귀|CoopControllerTest, CoopPlayInputTest, CoopPlayClient, CoopContentPathTest, Desktop의 CoopRenderSmoke 및 RenderEncounter|

세부 파일 경로는 검증 ZIP의 changed-files.txt에 있다. 함정·특수 문·보스 연출은 입력 동작과 수신자별 효과 규약을 함께 확장해야 한다. [지원표](CONTENT-COMPATIBILITY.md)

마법 지팡이의 조준은 해당 좌석의 불변 PublicDungeon과 아이템 핸들에 묶인다. 취소와 화면 버전 변경은 입력을 폐기한다. CoopBoard는 자기 좌석에 확정되어 연속 버전으로 전달된 발사 이벤트에서만 공개 표적을 향해 원본 MagicMissile 이펙트를 재생한다.

SYNC3의 공개 floor, stair 행으로 CoopScene이 현재 층, 조건 안내, 동의·취소·이동 버튼을 구성한다. 이동 중 표시와 입력은 호스트의 확정 영수증을 기다린다. 층이 바뀌면 이전 지도와 시각 효과의 기반을 버리고 검증한 전체 StateImage에서 지도를 다시 만든다.

## 하수도 방과 대화 입력

CoopBoard는 좌석의 PublicDungeon에서 직접 관측한 감시탑과 충전, 일반 피라냐, 벌, 악취나는 쥐와 악취 가스를 그린다. 유령을 직접 관측하고 인접한 좌석에만 대화 상태가 있다. CoopScene은 이 상태에 따라 기존 WndOptions에 원본 유령 대사와 무기·방어구 선택을 표시한다. 확인을 누르면 해당 공개 셀에 대해 SeatActions가 논리 행동을 만든다. 꿀단지 깨기는 자기 가방의 공개 핸들을 전달한다. 방을 재개하면 새 좌석 모델에서 몬스터와 대화 상태를 다시 그린다. [공개 상태](docs/coop/state-sync.md)

## 미믹 및 석상의 공개 상태 렌더링

CoopBoard는 중립 미믹과 재료가 같은 상자를 동일한 ItemSprite로 표시한다. 공개 몬스터 행이 도착하면 MimicSprite, MimicSprite.Crystal, StatueSprite, GnollExileSprite, PhantomPiranhaSprite의 원본 대기 애니메이션을 사용한다. 이 객체들은 Char에 연결되지 않고 원격 엔진이나 지도를 참조하여 좌표를 결정하지 않는다. 렌더링 위치는 검증된 PublicDungeon에 따른다. 탈취물 자체의 다른 좌석 인벤토리 정보와 보이지 않는 이동에는 화면 효과가 없다.
