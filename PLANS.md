# 단계와 다음 작업

0 원본 빌드·CI 준비 → 1 엔진 분석 → 2 원본 회귀 기반 → 3 오프라인 다중 영웅 → 4 LAN 로비 → 5 권위 턴·동기화 → 6 협동 규칙 → 7 저장·재접속 → 8 UI·전체 콘텐츠 → 9 스트레스·최종 APK → 10 실기기 및 후속 플랫폼.

## Phase 2 실행 순서

1. :core:test에 JVM 테스트와 libGDX 1.14.0 headless backend, 필요한 desktop natives를 test scope로만 추가한다. 임시 폴더에 FileUtils.setDefaultFileProperties를 설정하고 Gdx.app/files와 원본 assets 경로를 준비한다. 각 시드는 독립 JVM에서 실행하고 종료 시 actor 스레드를 join한다.
2. 수정 전 태그를 별도 checkout하고 동일 러너로 기준 데이터를 생성한다. GamesInProgress.selectedClass, Game.versionCode, Dungeon.seed를 지정한 뒤 Dungeon.init → newLevel → switchLevel을 실행한다. 생성 지형, 출입구, 아이템·몹 종류/위치, HP와 장비를 정렬해 기록한다. 생성기 스택 reset 뒤 전투 시드도 명시하고 벽시계·객체 주소는 비교에서 제외한다. 두 프로세스 결과가 다르면 기준 파일 승인을 보류한다.
3. Hero.ready의 sprite, onMotionComplete/onAttackComplete와 Actor.process의 wait/notify를 실제 호출 순서로 추적한다. 원본과 공유하는 단일 actor 선택/실행 단계를 추출하고 렌더링 완료 신호만 대체한다. 이동→공격→아이템→버프 만료를 기록해 actor ID, 행동 비용, 우선순위, HP·가방 변화를 태그 기준과 비교한다. 스케줄러 재작성과 랜덤 결과 하드코딩은 금지한다.
4. Dungeon.saveAll/loadGame/loadLevel로 한 층 왕복과 복원 후 후속 행동을 비교한다. Actor.fixTime 전후 절대 경과와 상대 순서를 비교하고 통계 시간까지 검사한다. 별도 시드의 Goo 층, 사망·앙크와 최종 패배, 승리 경로를 추가한다.
5. CI에서 생성된 JUnit XML의 실제 testcase 수와 실패 수를 확인한다. 핵심 생성·행동·저장 회귀가 통과한 후에만 Phase 3에 진입한다. Phase 4 이후 별도 호스트/클라이언트 프로세스 결과, 시야별 스냅샷, 지연·단절 및 자원 제한을 테스트 리포트에 추가한다.

## 검증 결과에 따른 다음 단계

Phase 2의 원본 엔진 실행 기반은 확보했다. Hero의 비용과 실제 Actor.process를 직접 실행하여 스케줄러를 분리할 필요가 없었다. 포크의 Actions에서 동일 스크립트의 9건과 실제 APK가 확인될 때 원격 CI 항목을 닫는다. Phase 3 첫 변경은 네트워크 없는 세션에서 두 영웅을 만들고 각각의 Belongings 및 위치가 독립적인지 검사하는 것이다. 이때 기존 싱글 회귀를 함께 실행한다.

## Phase 4 진입점

1. 동일한 엔진 실행 스레드 앞에 요청 큐를 둔다. UDP 발견·TCP 직접 연결·로비에서 정한 세션/플레이어/좌석을 엔진의 open/join 매핑에 연결한다. 크기·빈도·프로토콜 버전을 검사한다.
2. 호스트가 아이템 식별자를 객체로 해석하고 세션·좌석·Actor ID와 소유권을 재검사한다. 로컬 `act`의 입력 차례 검사를 재사용한다. 새 연결에서 이전 세션의 숫자 ID를 재사용해도 이전 행동이 승인되지 않아야 한다.
3. 별도 JVM의 실제 호스트·클라이언트로 접속/종료 및 로비 메시지를 검증한다. 이후 Phase 5에서 action ID별 확정 결과, state version, 수신자별 초기 스냅샷 및 재동기화를 구현한다. Java 객체·원본 전체 월드 직렬화를 네트워크에 보내지 않는다.
4. 협동 콘텐츠 확장 시 기존 단일플레이 9건과 Phase 3의 시드·장비·전투·시간·저장 격리 회귀를 함께 실행하고, 지원할 몹/아이템/지형을 추가할 때 해당 경로의 게스트 좌석 테스트를 먼저 보강한다.
