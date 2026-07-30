# TotemAutomata

TotemAutomata 讓原版銅魁儡成為可設定的分類與採集助手。玩家使用銅扳手
替每隻銅魁儡設定來源銅箱、目的地、工作區、燃料、工具、手動規則與
選配的 OpenAI-compatible LLM 判斷。

目前候選版本為 **0.1.6**，精確搭配 TotemCore **0.2.0**。

## 安裝

Client 與 Server 都放入：

1. Fabric API `0.154.2+26.2`
2. TotemCore `0.2.0`
3. TotemAutomata `0.1.6`

| 項目 | 需求 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Java | 25+ |
| 必要 Totem 模組 | `totem-core =0.2.0` |
| 選配 | TotemRemnant（可攜式容器安全 policy） |

Automata 不要求 DeadRecall、TotemRemnant 或 Cognition。使用 DeadRecall
2.4.4 整合 JAR 時不要再放入獨立 TotemAutomata。

## 合成銅扳手

工作台配方使用 3 個銅錠與 1 根木棒：

```text
_ C _
_ C C
S _ _
```

`C` 是銅錠，`S` 是木棒。

## 快速開始

1. 手持銅扳手直接右鍵銅魁儡；不需要蹲下。這會選取該銅魁儡並開啟
   管理 GUI。
2. 右鍵銅箱，設定唯一的來源／Home。
3. 在 GUI 放入合法燃料，選擇「分類」或「採集」。
4. 設定目的地或工作區後，按「運作」。

銅魁儡受傷時，可用銅錠右鍵修復 4 點生命；生存模式會消耗一個銅錠。

## 銅扳手操作

| 操作 | 分類模式 | 採集模式 |
| --- | --- | --- |
| 右鍵銅魁儡 | 選取並開啟 GUI | 選取並開啟 GUI |
| 右鍵銅箱 | 設定來源 | 設定 Home |
| 左鍵目前來源銅箱 | 解除來源 | 解除 Home |
| 右鍵一般容器 | 加入目的地 | 拒絕：採集模式不綁一般容器 |
| 左鍵已綁定容器 | 移除目的地 | — |
| 右鍵普通方塊 | — | 設定 Corner A |
| Shift+右鍵普通方塊 | — | 設定 Corner B |
| 左鍵普通非容器方塊 | — | 新增／移除該 Block ID 的手動目標 |

銅魁儡、來源、目的地與工作區必須位於同一維度。

## 分類模式

1. 停止銅魁儡並切換到「分類」。
2. 設定來源銅箱。
3. 依希望的優先順序右鍵目的地容器。
4. 放入燃料後啟動。

分類規則：

- 每次最多從來源取出 16 個物品。
- 目的地依綁定順序檢查。
- 有相同 Item 與 Data Components 的堆疊會優先合併。
- 空箱不會在沒有規則時自動接收所有物品。
- 目的地都拒絕時，物品會嘗試返回來源。
- 容器內的一般 Remnant 背包可作為目的地，但死亡背包不行。

## 採集模式

1. 停止銅魁儡並切換到「採集」。
2. 設定 Home 銅箱。
3. 設定 Corner A 與 Corner B。
4. 左鍵要採集的方塊種類，或設定採集 LLM 規則。
5. 在 GUI 放入合適工具與燃料後啟動。

限制與行為：

- 每軸最多 64 格，總體積最多 262,144 格。
- 只掃描已載入區塊，不會強制載入 chunk。
- 每 tick 最多檢查 512 個候選方塊。
- 排除容器、流體、不可破壞方塊與銅魁儡自己的 Home。
- 採集倉庫最多 16 個物品；滿載或沒有可合併目標時返回 Home。
- 只有成功破壞方塊後才消耗燃料與工具耐久。

切換模式前必須先停止；從採集切回分類前還要取出工具並清空採集倉庫。

## 選配 LLM

GUI 可為每隻銅魁儡設定 OpenAI-compatible Chat Completions：

- API URL、API Key、Model 與連線測試。
- 分類模式可為每個目的地設定獨立 Prompt。
- 接受／拒絕結果會快取，可在 GUI 手動調整。
- 採集模式有獨立 Prompt，手動目標優先於 LLM。

API 未設定、逾時或格式錯誤時會安全失敗，不阻塞 Server tick。不要把
API Key 放進公開截圖、issue 或 log。

## 疑難排解

| 狀態 | 優先檢查 |
| --- | --- |
| 缺少燃料 | 停止後在燃料槽放入合法燃料 |
| 找不到分類位置 | 來源、目的地、快取／Prompt 與容器容量 |
| 尚未設定作業區 | Corner A、Corner B 是否在同一維度 |
| 缺少工具／工具損壞 | 停止後更換適用工具 |
| 倉庫已滿 | Home 是否已載入且能接收物品 |
| 資料已更新 | 重新開啟 GUI，以 Server 最新 snapshot 操作 |

## 開發與驗證

```bash
./gradlew build
```

Client 視覺測試：

```bash
./gradlew runClientGameTest
```

候選版已通過 16/16 required Fabric GameTests、三 JVM legacy-world
migration，以及 headless Client GUI 截圖 gate。截圖在
[`test-artifacts/screenshots/`](test-artifacts/screenshots/)；所有權與
cutover 契約見 [EXTRACTION.md](EXTRACTION.md)。
