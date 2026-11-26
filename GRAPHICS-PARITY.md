# 원본과 협동 던전 화면 대조

## 전직과 갑옷 능력 체크포인트

한국어·영어 Desktop의 원본 콤보 메뉴, 조준·취소·메치기, 왕관 선택과 변환 후 화면을 확보했다. [실행 결과](docs/coop/combo-results.md)

영어·한국어 Desktop에서 원본 정령 속성 스프라이트와 보이는 공격 예고, 귀환 제안, 지상 승리를 촬영했다. 픽셀 일치나 Android 보스전은 별도 검증 대상이다. [실행 결과와 다음 작업](docs/coop/vault-ascent-results.md)

최종층은 HallsBossLevel과 LastLevel의 원본 바닥·벽 프레임과 좌석 시야를 사용한다. 1364×1024 한국어·영어 실제 Desktop에서 광선 예고, 첫 주먹, 26층 승리 제안 및 실제 클릭 후 승리 화면을 촬영했다. 보이는 광선 칸만 각 좌석에 전송한다. [최종 보스·승리 검증 기록](docs/coop/yog-victory-results.md)

동일한 1364×1024 Desktop 화면에서 원본 도시 금고와 악마의 전당 타일·수면, 좌석별 안개와 HUD를 확인했다. 위치·전력은 배치형 시험 조건이다. [한국어 금고](docs/coop/imp-halls/ko/imp-vault.png) · [영어 금고](docs/coop/imp-halls/en/imp-vault.png) · [한국어 21층](docs/coop/imp-halls/ko/halls-21.png) · [영어 21층](docs/coop/imp-halls/en/halls-21.png)

## 광산·도시 체크포인트

같은 깊이에서 광산으로 이동하면 지도·안개·카메라를 새로 설정한다. 원본 광산 타일, 도시 수면과 상인을 한국어·영어 Desktop에서 촬영했다. 시드 572의 원본 왕좌 방에서 보호막 단계와 보스 처치 직후도 두 언어로 촬영했다. 협동 호스트의 로컬 업적 배너가 좌석 화면 전체를 가리던 문제를 수정했다. 두 언어에서 원본 대장장이 앞에서 제작 대금을 지불하고 보상 후보를 선택하는 화면을 추가했다. 14개 Desktop 화면의 위치와 장비는 시험용 배치다. [실행 기록](docs/coop/mines-city-results.md)

## 동굴과 DM-300 · Desktop 실측

원본 Spinner, Blacksmith, DM-200, DM-300과 Pylon 스프라이트 및 동굴 보스방 atlas를 사용한다. 수신자에게 보이는 거미줄·전격과 원본 벽 위 장식은 현재 좌석 투영에서 그린다. 영어·한국어 화면은 seed 570, 시험용 위치와 장비·체력, 소프트웨어 OpenGL 실행 결과다. 숨은 필드의 연출, 전체 원본 음향과 Android 동굴 렌더링은 미검증이다. [한국어](docs/coop/caves-dm300/ko/dm_charge.png) · [영어](docs/coop/caves-dm300/en/dm_won.png)

## Tengu와 원본 동굴 입구

시드 570의 시험 배치에서 Xorg 소프트웨어 OpenGL로 두 좌석을 실행했다. 한국어와 영어로 각각 10층 도착, 첫 전투방, 두 번째 전투장, 승리 후 방, 11층 도착을 캡처했다. 원본 v4.0.2의 전투방·전투장 캡처도 통합 ZIP에 있다. 비교 대상의 영웅 위치·상태가 달라 픽셀 일치율을 산출하지 않았다. 원본 출구 커스텀 타일과 좌석별 안개, HUD·퀵슬롯은 실제 협동 화면에 표시된다. 동일 해상도의 공식 전투장 화면에는 상단 중앙 보스 체력 바가 있다. 협동 캡처에서는 그 자리가 비어 있고, 두 번째 영웅과 동적 전투 구역이 표시된다. 개별 공격 소리와 모든 파티클의 일치는 미검증이다. [한국어 전투장](docs/coop/tengu-caves/ko/tengu-fight_arena.png) · [영어 11층](docs/coop/tengu-caves/en/prison-11-arrival.png)

## 감옥 8·9층 실제 Desktop 화면

한국어·영어 Desktop에서 시드 570의 원본 감옥 타일, 좌석별 안개, 원본 강령술사 스프라이트, HUD, 퀵슬롯과 층 이동 버튼을 확인했다. 두 좌석의 실제 LAN 컨트롤러를 사용하고 전투 위치·계단은 시험용으로 배치했다. 통합 백업의 screenshots/에 양 언어의 8·9층 도착 및 강령술사 6장을 보존한다. [한국어 강령술사](docs/coop/prison-nine/ko/prison-necromancer.png) · [영어 9층](docs/coop/prison-nine/en/prison-9-arrival.png) Tengu 화면, 실제 Android 렌더링과 터치는 NOT RUN이다.

## 두 보상 선택과 휴대 지팡이 조준

한국어와 영어에서 원본 두 후보를 확인하고 타락 지팡이를 선택했다. 별도로 배치한 마력탄을 4번째 퀵슬롯에 등록해 조준·취소했다. 실제 PNG에는 원본 비교 자료와 이번 시험 배치 조건이 함께 있다.

## 7층 원본 지팡이 장인

시드 577의 시험 배치에서 영문·한국어 Desktop 입력으로 장인 스프라이트를 선택하고 썩은열매 의뢰를 수락했다. 원본 NPC/아이템 스프라이트·번역과 WndOptions를 사용한다. 제공하는 PNG의 배치 조건은 의뢰 검증 기록에 있다. 최초 의뢰판 촬영은 수락까지이며 보상 선택은 후속 5e22a74 촬영에 있다.

## 감옥 상점과 7층

공식 v4.0.2의 GameScene과 WndTradeItem에서 6층 상점 및 구매 창을 실제 Desktop에 띄웠다. 협동 화면은 같은 원본 그래픽에 좌석별 가격·수량·잔액과 본인 환매를 표시한다. 두 언어에서 실제 구매·판매·환매, 단검 조준, 계단 동의 후 7층 진입을 촬영했다. 상점 및 계단은 시험 배치를 사용하며 원본과 협동 화면의 지도·캐릭터 위치는 다를 수 있다. 공식 원본의 PrisonBossLevel도 시험용 위치에서 FIGHT_START·FIGHT_PAUSE·FIGHT_ARENA를 실행해 1차 전투방과 2차 전투장을 두 언어로 촬영했다. 이 사진은 원본 비교 전용이다. 체인과 전격의 전용 연출은 남아 있다.

## 이전 상점 체크포인트 · P1-A

감옥 상점은 원본 감옥 타일맵, 상인 스프라이트, ItemSprite, WndOptions와 기존 격자 가방을 사용한다. 영·한 Desktop에서 실제 포인터 입력으로 잔액 부족, 취소, 구매·판매·환매를 확인했다. 상점 시험은 생성된 상품 인근으로 배치한 장면이다. 원본 WndTradeItem의 완장 절도 버튼은 미지원이다.

공식 v4.0.2의 Desktop 화면을 기준으로 협동 던전의 일반 플레이 경로를 대조했다. 변경 전 커밋은 `011364de12d732fa6eb63e426a3ecec5ea4283a1`, 공식 태그의 커밋은 `57a4e06a4caf162446d1c28caa7983f0493fecf0`이다. 그래픽 체크포인트는 V1 `8ddf53b`, V2 `b00303e`, V3 `e4162cb`, 부분 갱신 검증을 포함한 APK 소스는 `12191be`다.

## V4·V5 입력과 감옥 6층

애플리케이션 `2e41a67`과 입력 시험 드라이버 `71491b1`을 사용했다. 1364×1024 Linux Desktop에서 영어·한국어로 실제 협동 클라이언트를 조작했다. 빈 슬롯을 눌러 가방의 아이템을 등록하고, 길게 눌러 교체하거나 비웠다. 가방의 실제 하위 탭, 아이템 소진 표시, 물약 사용, 활·돌 조준 취소와 발사, 강화·감정 주문서의 장비 선택을 확인했다. 표적 선택과 카메라 조작은 렌더러에만 적용된다. 발사 연출은 현재 좌석의 확정 입력에 연결한다.

| 장면 | 실제 화면에서 확인한 항목 |
| --- | --- |
| [한국어 사용자 지정 퀵슬롯](docs/coop/graphics-parity/quickslots-ko.png) | 네 원본 도구 버튼 위 자기 아이템과 수량. 소진 뒤 희미한 슬롯 및 교체 조작은 검증 ZIP에 포함된다. |
| [영어 강화 대상 선택](docs/coop/graphics-parity/upgrade-target-en.png) | 자기 가방의 격자에서 장비 선택 또는 취소. 미감정 장비의 강화값은 공개 전까지 숨긴다. |
| [한국어 감옥 6층](docs/coop/graphics-parity/prison-entry-ko.png) | 원본 감옥 바닥·벽·수면·좌석 안개와 동일 HUD. 이 화면은 계단과 전투가 준비된 렌더링 시험에서 촬영했다. 정상 시작 완주 수치는 별도의 소켓 시험에 있다. |

기존 원본 v4.0.2 기준 화면과 V1–V3 자료를 아래에 보존한다. 카메라 관성·핀치, 전체 특성/전직 조작, 동료의 확정 전투 연출과 일부 환경 파티클은 남아 있다. [시험 보고](TEST_REPORT.md)

## 원본에 비해 검게 보이던 셀

시드 402의 입구 방에서 변경 전 협동 화면은 캐릭터 오른쪽과 방 안쪽의 물을 어두운 배경색으로 표시했다. 원본과 양쪽 좌석의 방에는 물 지형이 존재한다. 아틀라스는 256×256, 각 프레임은 16×16, 한 줄에 16개다. 바닥 0번은 알파가 전부 255이고 기본 물 32번은 256픽셀 모두 알파가 0이다. 인접 육지를 이은 물 가장자리 프레임은 일부 픽셀만 불투명하다.

변경 전 보드는 물 32번을 단독 Image로 생성했다. 원본 GameScene은 먼저 스크롤되는 water0 표면을 그리고 그 위에 DungeonTerrainTilemap으로 물가를 잇는다. 같은 방에서 투명 픽셀 아래에 나타나는 색을 결정한 차이다. 이전 보드의 평면 벽 선택도 원래의 벽 높이와 문 위 가림을 잃게 했다. 신규 화면에서는 알려진 셀의 네 이웃으로 타일을 잇고 높이·그림자·안개를 순서대로 적용한다. 미발견 셀은 계속 가린다.

## 실제 실행 경로

| 원본 코드 | 협동에서 재사용한 부분 | 입력과 상태의 소유자 |
| --- | --- | --- |
| GameScene, SkinnedBlock, DungeonTilemap, DungeonTileSheet, DungeonTerrainTilemap | 물 배경의 원본 텍스처와 이동 속도, 바닥 변형, 물·낭떠러지 연결, 높이가 있는 벽과 문 | 단일 플레이는 Level의 배열, 협동은 좌석별 DungeonRenderMap |
| WallOcclusionTilemap, TerrainFeaturesTilemap, RaisedTerrainTilemap, DungeonWallsTilemap, WallBlockingTilemap, FogOfWar | 바닥 그림자, 식물·풀·보이는 함정, 캐릭터 앞의 벽 및 높이, 벽면 절반 가림, 현재·방문·매핑·미발견 안개 | 협동에서 지형·기억은 검증된 PublicDungeon에 있는 셀만 사용 |
| CustomTilemap, GooBossRoom.GooNest | 원본과 같은 둥지 프레임 선택 | 호스트가 직접 보거나 방문한 셀의 고정 장식 프레임만 공개 |
| HeroSprite, CharSprite, ItemSprite, GooSprite | 원본 계층별 자세·전사/사냥꾼/마법사 시트, 턴과 무관한 표시용 위치 계산, Goo 충전 자세 | 화면은 Char나 Heap에 링크하지 않는다. 실제 피해·시간·Actor는 호스트가 소유 |
| StatusPane, HeroSprite.avatar, BuffIcon | 초상화 프레임, 체력·보호막·경험치, 레벨, 버프와 현재 층 | 자기 좌석의 공개 값과 Hero.maxExp로 표시 |
| Toolbar.Tool, ItemSprite | 원본 버튼 배경과 눌림 상태, 대기·탐색·줍기·가방, 네 칸의 빠른 사용과 조준 | CoopController와 SeatActions가 기존 식별자와 턴을 검증 |
| WndBag, WndTabbed, InventorySlot, ItemSlot, IconTitle, WndOptions | 원본 5열 배치 계산, 장비 자리, 수량·강화 수치, 가방 테두리·탭, 공개 설명과 행동창 | WndSeatInventory는 자기 가방의 공개 핸들만 사용 |

원본 StatusPane, Toolbar, QuickSlotButton, WndBag에는 Dungeon.hero 및 전역 가방·퀵슬롯에 직접 명령하는 코드가 있다. 협동은 위 표의 표시 구성 요소를 공유하고 입력을 기존 좌석 컨트롤러에 연결했다. 원본 GameScene의 필드 값을 교체하지 않는다. 원본 방 전체의 비공개 배열이나 RNG 상태는 클라이언트에 전송하지 않는다. 장식 변형은 세션 ID와 층에서 파생한 로컬 그래픽 난수를 사용하므로 같은 종류의 바닥이라도 원본의 개별 무늬와 다를 수 있다.

동일 층에서는 카메라, 터치 영역, 물, 타일맵, HUD 및 계속 보이는 개체를 유지한다. 지형·공개 특성·시야가 달라진 셀의 경계 상자를 두 칸 확장하여 인접 타일, 안개, 벽 가림을 갱신한다. 폴링 자체는 움직임이나 공격 효과를 시작하지 않는다. 층 전환 시에는 해당 층의 표시 객체와 카메라를 해제한다.

## 촬영과 비교 방법

공식 태그의 별도 worktree에서 Gradle로 Desktop release JAR를 만들었다. 외부 시험 드라이버는 이 JAR에 대해 컴파일하고 실제 GameScene, WndBag, WndHero를 열었다. 변경 전·후 협동 화면은 실제 LWJGL, X11, 소프트웨어 OpenGL과 독립 게스트 JVM을 사용했다. 원본 PNG는 1364×1024이며 주 비교 언어는 영어다. 최종 협동 화면은 한국어도 촬영했다. 공식 비교에서는 첫 실행 튜토리얼을 종료한 컴팩트 HUD와 3배 배율을 사용했다. 기본 시드 402의 1층은 전사, Goo 비교는 마법사다.

| 비교 장면 | 캡처에서 확인할 부분 | 조건 차이 |
| --- | --- | --- |
| 일반 하수도·물 | 입구 방 바닥, 물결, 물가, 위·옆벽과 시야 경계 | 협동은 두 영웅이 서로 다른 셀에 선다. 장식 분산과 아이템 배치는 달라질 수 있다 |
| 벽·잠긴 문·안개 | 실제 생성된 잠긴 문 앞의 높이와 그림자, 시야 밖에 남은 입구의 지형과 미탐험 영역 | 비교용으로 영웅을 문 인접 셀에 배치했다. 협동 다트·문 해제 캡처의 조작은 실제 TCP로 수행했다 |
| 5층 Goo | 물, 보스 둥지, 벽과 기둥, 시야에 들어온 보스 및 충전 | 공식은 Goo 근처에서 시작한다. 협동은 준비된 계단·보스전 시험 경로를 사용한다. 화면의 전투를 정상 시작 완주 집계에 더하지 않는다 |
| HUD·가방·캐릭터 | 원본 게이지와 테두리, 장비 자리·수량·실제 아이템 설명, 캐릭터/장비별 외형 | 협동의 차례 표시가 추가된다. 소유한 물품과 가방 내부 정렬은 달라질 수 있다 |

같은 크기의 전체 화면을 열어 물 표면, 문의 높이, HUD와 가방을 비교할 수 있다.

| 장면 | 공식 v4.0.2 | 변경 전 협동판 | V3 협동판 |
| --- | --- | --- | --- |
| 입구 방 · 물 · 시야 | [원본 하수도](docs/coop/graphics-parity/original-sewer.png) | [변경 전 하수도](docs/coop/graphics-parity/before-sewer.png) | [현재 하수도](docs/coop/graphics-parity/coop-sewer.png) |
| 잠긴 문 | [원본 문](docs/coop/graphics-parity/original-door.png) | [변경 전 문](docs/coop/graphics-parity/before-door.png) | [현재 문](docs/coop/graphics-parity/coop-door.png) |
| 인벤토리 | [원본 가방](docs/coop/graphics-parity/original-bag.png) | [변경 전 가방](docs/coop/graphics-parity/before-bag.png) | [현재 가방](docs/coop/graphics-parity/coop-bag.png) |
| Goo 보스방 | [원본 Goo](docs/coop/graphics-parity/original-goo.png) | [변경 전 Goo](docs/coop/graphics-parity/before-goo.png) | [현재 Goo](docs/coop/graphics-parity/coop-goo.png) |

비교 ZIP의 `comparison`에는 실제 화면에서 상단 왼쪽 246×114픽셀의 같은 좌표를 잘라 구한 채널별 절댓값 차이 PNG와 수치가 있다. 전체 원본 컴팩트 상태창 영역의 28,044픽셀 중 RGB가 일치한 수는 변경 전 0, 최종 협동 15,524였다. 평균 채널 절댓값 차이(0–255)는 39.00에서 18.89로 줄었다. 이 영역에는 협동 차례 문구도 포함된다. 카메라 위치·시야·월드 상태가 다른 방 전체에는 이 수치를 적용할 수 없다.

싱글플레이 보존을 별도로 확인하기 위해 같은 촬영 드라이버를 최종 Desktop 빌드에도 실행했다. 공식 원본과 최종 싱글플레이의 전체 1364×1024 화면을 비교한 평균 채널 절댓값 차이는 입구 방 0.2754, 잠긴 문 0.0352, 가방 0.0036, 캐릭터 창 0.0001, Goo 0.189였다. 독립 실행이므로 움직이는 물·캐릭터의 촬영 시각은 다를 수 있다. 픽셀 집계는 ZIP의 `comparison/singleplayer.json`에 수록했다.

## 에셋, 증거와 남은 차이

- [원본과 바이트 단위로 대조한 에셋 22개의 경로와 SHA-256](docs/coop/graphics-assets.sha256)
- [빌드 로그, JUnit XML, 장면별 원본 PNG와 비교 자료](docs/coop/graphics-parity-evidence.zip)
- [실행 명령과 재현 조건](BUILD.md)
- [시험 판정](TEST_REPORT.md)

V3 당시 네 기본 동작은 시작 지팡이·활·식량·회복 물약이었다. V4·V5에서 개인 배치, 길게 눌러 교체 및 연속 휴식을 추가했다. 원본의 추가 퀵슬롯 페이지는 후속 범위다. 장비와 소지품은 한 좌석 안에서 확인할 수 있다. 원본의 모든 하위 가방별 분류, 캐릭터 정보의 특성·통계 탭, 예정된 지속 피해 음영, 각 몬스터의 전투 애니메이션 전체, 환경 파티클·잔물결 및 전체 커스텀 타일의 일치는 미검증이다. Android 설치·터치·화면 회전·무선 LAN은 NOT RUN이다.
