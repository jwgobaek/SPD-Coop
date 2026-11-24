# 협동 화면과 실행 스레드

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
