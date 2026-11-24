# 하수도 협동 지원 범위

지원 빌드는 branch 0의 1층과 2층을 실행한다. 402–423의 연속된 22개 시드에서 원본 Dungeon.newLevel을 사용해 각 1–5층을 생성했다. 조사기는 해당 층의 원본 생성 코드를 직접 호출한다. 생성된 방을 걷거나 보스를 공격하는 시험은 별도로 구분한다. TSV의 크기는 층만 담은 비압축 UTF-8 Bundle이며 영수증·영웅을 포함한 디스크 저장 세대 크기는 TEST_REPORT.md에 있다. [110개 생성 결과](sewer-generation.tsv)

| 시드·층 | 실제 원본 생성과 확인한 동작 | 시험 경계 |
|---|---|---|
| 402·2 | 감시탑이 방 안의 노출된 한 영웅을 충전·공격. 표적 변경 시 재충전, 사망 직전 HP, 충전 저장·복원, 다른 좌석의 HP와 비공개 행 검사 | 양 좌석 엔진 시험, TCP·Desktop에서 교전 위치 준비 |
| 403·2 | 원본 유령 대화, FetidRat 생성·원본 체력·마력탄 전투, 악취·점액, 무기 또는 방어구 한 번 지급, 수령 확정 직후 호스트 종료 후 영수증 재조회 | 두 좌석 보상 선택을 번갈아 검증. 대화·전투 위치는 시험 배치, TCP와 Desktop 대화에서는 유령의 위치를 고정 |
| 406·2 | 일반 피라냐 물속 이동·공격, 층 왕복 후 Actor ID·위치·HP, 원본 고기 전리품 | 대상 지정·교전 위치를 준비. 전리품 단위 시험은 직접 치명상을 가함 |
| 409·2 | 비밀 꿀단지 방의 원래 벌·조각, 추가 꿀단지 획득·깨기·소환, 상대 좌석 공격, 조각을 다른 층에 놓은 후 복원 | 실제 방과 실제 벌, 위치를 준비한 엔진·TCP 시험. 폭탄과 투척은 제외 |
| 415·1↔2 | 이동·근접·마력탄·탐색·드롭·줍기·양 좌석 계단 동의, 응답 전 호스트 종료와 새 JVM 복원 | 원본 지도를 읽는 테스트 안내기가 정상 입력을 선택. 위치 직접 설정 없이 진행한 회귀 경로 |
| 408·2 / 411·2 / 416·2 | 일반 미믹 / 수정 미믹 / 석상 생성 | 세 시드 모두 실제 계단 거부·원복을 재확인 |
| 모든 시드 3–5 | 원본 구역 생성, 5층 SewerBossLevel | 협동 저장·진입·Goo 처치·보상·6층 진입은 NOT RUN |

22개 시드 모두에서 두 영웅을 1층 출구 옆에 배치한 추가 시험을 실행했다. 15개 시드에서는 실제 동의·층 이동과 엔진 상태 복원 후 1층 복귀·다음 행동에 성공했다. 405·408·411·413·416·420·421에서는 미지원 목적지를 거부했다. 그 경우 기존 1층 지도·턴·RNG를 대조하고 복원 후 다시 행동했다. TSV의 placed_gate_result, normal_route, restored_at_this_depth는 이 시험과 시드 415의 정상 입력 경로를 구분한다. 시드 415 이외에 계단까지 정상 행동만으로 이동하는 경로는 NOT RUN이다.

## 원본 경로와 다음 의존성

- SentryRoom.Sentry는 직접 시야·방 내부의 EMPTY_SP·소지품 상실 상태로 노출을 판정한다. Piranha는 원본 수중 이동과 전리품 경로를 따른다. 벌은 SecretHoneypotRoom.paint의 원본 개체와 Honeypot.shatter의 새 소환을 구분한다. 실제 꿀단지 조각과 기존 벌 사이의 연결은 Actor ID로 추적한다. 꿀을 먹여 아군으로 전환한 벌의 추종·소유권은 이번 범위에서 검증하지 않았다.
- SewerLevel.createMobs와 Ghost.Quest.spawn이 월드당 의뢰 층을 정한다. 2층 FetidRat은 악취와 점액 효과를 사용한다. 3층 놀 사기꾼·4층 거대한 게 의뢰는 별도 원본 행동 검증이 필요하다. 현재 보상 귀속은 DECISIONS.md의 임시 규칙이다.
- PoolRoom.paint에서 Piranha.random을 호출해 희귀 변종도 생성할 수 있다. 일반 피라냐 검증 결과는 PhantomPiranha에 적용하지 않는다. CrystalVaultRoom·StatueRoom·SecretHoneypotRoom 등 원본 방별로 초기 Actor를 별도 조사했다.
- 일반 미믹 및 CrystalMimic은 중립 상자 상태와 공개 후 표적·공격 시간을 구분해야 한다. CrystalMimic.steal은 가방에서 한 개를 분리하여 미믹 내부 목록으로 옮긴다. 휴면 층의 소유권 검증에도 이 목록과 꿀단지 조각을 포함해야 한다. 순간이동·도주·사망·재접속 후에도 같은 아이템 ID를 추적하는 시험이 다음 작업이다.
- SewerLevel.trapClasses는 1층의 WornDartTrap, 이후 Chilling·Shocking·Toxic·WornDart·Alarm·Ooze·Confusion·Flock·Summoning·Teleportation·GatewayTrap을 선택한다. 방 생성기도 함정을 배치해 표본에는 BurningTrap, GrippingTrap, RockfallTrap 등이 추가로 나타났다. 함정 발동·잠긴 문·열쇠 소비는 별도 S2 범위다.
- 생성 표본에서 3층에는 Crab·Swarm, 4층에는 Slime·CausticSlime·Thief·PhantomPiranha가 추가로 나타났다. GnollExile은 2층에서도 발견됐다. 단순히 층 번호 한도를 올리면 이들의 AI·상태 효과·인벤토리 처리와 저장 경로를 거치게 된다.
- 원본 1층의 WornDartTrap은 처음부터 보인다. activate가 VFX_PRIO Actor를 등록하고, 발동 칸에 있는 Char 또는 원본 Ballistica 경로의 가까운 Char를 선택한다. 원본 피해는 4–8 주사위에서 대상 방어 굴림을 뺀다. 협동에서는 렌더링 콜백 없이 해당 Actor 단계에서 한 번만 피해를 적용하고 확정 전 콜백 잔류 여부를 검사해야 한다.
- 원본 Key.doPickUp은 열쇠를 Notes에 추가하고, Hero는 잠긴 문의 층별 IronKey 수량을 확인한 뒤 키를 제거한다. 공용 열쇠 기록을 유지하는 방법과 좌석별 소지로 바꾸는 방법이 후보이며 최종 규칙은 미정이다.
- SewerBossLevel.createMobs는 비어 있고, DiamondGooRoom 등의 paint에서 Goo를 직접 배치한다. RatKingRoom.paint가 쥐 대왕을 추가한다. SewerBossLevel.seal/unseal과 Goo.doAttack/damage/die/storeInBundle을 조사했다. 전투에서 원본이 사용하는 충전 단계 pumpedUp, 물 회복과 저체력 공격, 봉쇄 전환, WornKey 한 개와 GooBlob 추첨을 각각 확인할 필요가 있다. Goo.die의 원본 보상은 주사위 결과에 따라 2–4개 점액이다. 협동 보상 분배와 경험치 정책은 미정이다. 기존 싱글플레이 BossRegressionTest는 직접 피해를 주어 봉쇄·해제와 WornKey 드롭을 시험한다. 이 시험은 협동 보스전 완주 집계에서 제외한다.

각 시나리오의 실제 처리 횟수와 Android·Desktop 검증은 [테스트 보고서](../../TEST_REPORT.md), 층 이미지·백업·내구성 한도는 [저장 명세](save-format.md)에 있다.

## S4 저장 크기 조사

22개 시드에서 실제 생성한 원본 층 Bundle 110개의 UTF-8 크기는 23,971–52,130 B, 중앙값 31,585 B, p95 42,492 B였다. 시드별 다섯 층의 합계 중앙값은 163,651 B, p95는 176,305 B, 최댓값은 180,752 B였다. 이는 처음 생성했을 때의 지형·몹·전리품이다. 탐험 기억, 장기 플레이 중 쌓인 아이템·버프, 좌석, 영수증, 세대 메타데이터의 증가량은 별도 부하 시험이 필요하다.

이번 버전은 원본 스키마 2의 전체 세대 4 MiB·휴면 레코드 한 개 1,500,000 UTF-8 바이트·정상 세대 2개 제한을 유지한다. S4에서는 branch/depth별 생성 이력, 전역 Actor·아이템 ID 중복, 휴면 층별 체크섬, 세대 간 참조 수명을 먼저 검증한다. 저장 슬롯을 확장할 때 실제 스키마 1·2에서 옮긴 뒤 양쪽 좌석을 인증하고 응답 전 강제 종료 및 백업 복구를 반복해야 한다.
