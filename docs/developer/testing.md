# 測試與回歸驗證

DeadRecall 使用三層自動測試：純 JVM 的 JUnit、會啟動 Minecraft GameTest Server 的 Fabric GameTest，以及使用正常 Dedicated Server world 的跨程序重啟探針。

## JUnit

適合測試不需要完整世界生命週期的純邏輯，例如：

- 映射表完整性。
- 成本與折抵公式。
- 狀態分類與訊息 key。
- JSON 資源結構。

執行：

```bash
./gradlew test
```

## Fabric Server GameTest

GameTest 原始碼位於：

```text
src/gametest/java/
src/gametest/resources/fabric.mod.json
```

測試模組 ID：

```text
deadrecall-gametest
```

Loom 設定使用 `fabricApi.configureTests` 建立獨立 `gametest` source set。Server GameTests 已接入 `build`，因此以下指令會同時執行 JUnit、編譯模組並啟動 GameTest Server：

```bash
./gradlew build --no-daemon --stacktrace
```

只執行 GameTest Server：

```bash
./gradlew runGameTest --no-daemon --stacktrace
```

目前 Client GameTests 保持停用；需要畫面、滑鼠或 Client-only 渲染驗證時，應另行建立 Client GameTest 或人工測試矩陣。

## 銅傀儡回歸

銅傀儡的 Server GameTests 分為三組：

```text
CopperGolemRegressionGameTest
CopperGolemPersistenceGameTest
CopperGolemTransportGameTest
```

目前自動覆蓋：

- 模式切換的 stopped、carried cargo、pending source、工具、倉庫與 active target 拒絕矩陣。
- 成功切換後的 revision、activity、scanner state 與 AI memory 重設入口。
- 採集倉庫 16 個上限、Data Components 保存，以及不同 item／component 的拒絕。
- 工作區軸長、體積與跨 Dimension 角點重設。
- 手動採集規則優先於 cached LLM deny。
- 未綁定板手、偽造 UUID、距離與 running slot-edit 權威檢查。
- 兩名玩家的板手綁定彼此隔離。
- Copper Golem Entity NBT round-trip 的 mode、running、revision、工具耐久、倉庫 Components、區域與手動規則。
- 真實 Chest source／destination 的 16 個取貨、來源 exactly-once 回滾與目的地存放。
- Home 滿載 preflight、相容 stack 合併與工具最後耐久的原子結果。

仍需保留在 OpenSpec 的項目包括非同步 LLM 過期回應、完整自主採集／尋路、區塊卸載、Copper Golem 跨 JVM world probe，以及多人同 tick 壓力競態。

## Dedicated Server restart probe

死亡背包的 world／entity／SavedData 重啟驗證不能只靠單次 GameTest 或 codec round-trip。專案另有 Loom run configuration：

```text
runRestartProbe
```

它使用正常 Dedicated Server 啟動流程與固定的：

```text
run/restartProbe/world
```

測試探針位於 `gametest` source set，不會被打包進正式模組 JAR。CI 會以三次獨立 JVM 執行：

1. `seed`：保存 ACTIVE death node、discovery 與綁定的 death-backpack ItemEntity。
2. `recover`：重新載入世界，以相同 UUID 的 replacement `ServerPlayer` 回收背包並停用節點。
3. `verify`：再次重新載入，確認節點仍為 `DISABLED`、探索資料存在，而且已刪除的背包實體不會復活。

每個 phase 都使用環境變數啟用：

```text
DEADRECALL_RESTART_PROBE_PHASE
DEADRECALL_RESTART_PROBE_MARKER_DIR
```

Probe 會 force-load 測試 chunk、等待 entity manager 完成載入，執行操作後再等待保存邊界，最後使用正常 `MinecraftServer.stopServer()` 路徑關閉。每一階段必須產生 success marker；entrypoint 未執行、world 沒有重用、entity region 未載入或 SavedData 狀態錯誤都會讓 CI 失敗。

此探針驗證實際 Dedicated Server world reload，但不模擬真人 Client 網路登入、封包交換或 UI 操作。

## 混凝土粉末回歸

`ConcretePowderItemHardeningGameTest` 驗證：

- 水源會使掉落物形式的混凝土粉末硬化。
- 非水源流動水也會硬化。
- 隔著方塊靠近水但沒有接觸時不會硬化。
- 世界正在下雨，但物品沒有浸水時不會硬化。
- 64 格數量與自訂名稱 Component 保留。
- 使用同一個 ItemEntity，不建立替代實體。
- age、位置範圍、速度與 pickup delay 不會被轉換流程重設。

## CI

GitHub Actions 使用 Java 25 執行完整 `build`，接著執行三階段正常 Dedicated Server restart probe。失敗時會上傳：

```text
build.log
restart-*.log
restart-probe/
**/build/reports/
**/build/test-results/
run/gametest/
run/restartProbe/
```

新增或修改 Mixin、世界生命週期、Entity、BlockEntity、SavedData 或網路流程時，至少應新增一項可自動重現的 JUnit、GameTest 或正常 Dedicated Server probe。真人多人測試與壓力測試不能由目前的自動 Server 測試完全取代，仍需保留在 OpenSpec 驗收矩陣中。
