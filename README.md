# TotemAutomata

TotemAutomata 讓原版銅魁儡成為可設定的分類與採集助手。玩家使用銅扳手
替每隻銅魁儡設定來源銅箱、目的地、工作區、燃料、工具、手動規則與
選配的 OpenAI-compatible LLM 判斷。

目前版本需要 TotemCore `>=0.7.13 <0.8.0`；除了 Automata 自有 production
Screen 的唯讀 semantic provider contract，0.7.13 也提供共用世界框線 API。

## 安裝

Client 與 Server 都放入：

1. Fabric API `0.154.2+26.2`
2. TotemCore `0.7.13`（支援 `>=0.7.13 <0.8.0`）
3. TotemAutomata `0.1.19`

| 項目 | 需求 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Java | 25+ |
| 必要 Totem 模組 | `totem-core >=0.7.13 <0.8.0` |
| 選配 | TotemRemnant（可攜式容器安全 policy）；TotemExcavation `0.1.5+`（錘子採集）；TotemLocksmith（鎖網路權限） |

Automata 不要求 DeadRecall、TotemRemnant 或 Cognition。DeadRecall bundle
已停止維護，也不會由這次更新重新啟用；新安裝請直接使用獨立 Totem 模組。

## 合成銅扳手

工作台配方使用 3 個銅錠與 1 根木棒：

```text
_ C _
_ C C
S _ _
```

`C` 是銅錠，`S` 是木棒。

## 快速開始

1. 手持銅扳手直接右鍵銅魁儡；不需要蹲下。這會選取該銅魁儡並開啟管理 GUI。
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
- 普通熔爐、煙燻爐與高爐會分別查詢自己的 RecipeManager 配方；只有能
  產生非空結果的物品才會放入 slot 0，上方輸入格不再接受無法處理的物品。
  slot 1 仍只收燃料、slot 2 仍是輸出格；一般容器的分類規則不變。

## 採集模式

1. 停止銅魁儡並切換到「採集」。
2. 設定 Home 銅箱。
3. 設定 Corner A 與 Corner B。
4. 左鍵要採集的方塊種類，或設定採集 LLM 規則。
5. 在 GUI 放入合適工具與燃料後啟動。

限制與行為：

- 每軸最多 64 格，總體積最多 262,144 格。
- 只掃描已載入區塊，不會強制載入 chunk。
- 每隻需要搜尋的銅魁儡每 tick 最多檢查 32 格，全伺服器合計最多 256 格；
  預算會輪替分配，避免固定順序造成飢餓。
- 已選定且仍有效的目標優先處理，不會同時重新掃描。每隻銅魁儡每 tick
  最多讓一個 cheap candidate 進入尋路、掉落、容量、權限與 LLM 等昂貴驗證。
- 相同採集目標與 Home 會沿用現有路徑；正常最多每 10 ticks 重算一次，
  每次只對一個目的地要求路徑。
- 排除容器、流體、不可破壞方塊與銅魁儡自己的 Home。
- **採集背包是共享總容量 16 個物品，可同時攜帶多種 Item/Data Components。** 例如石頭、煤炭、Raw Iron 與 Raw Copper 可以共存在同一次採集中，只要總數不超過 16。
- GUI 會把不同攜帶種類分開顯示；運作中只能查看，停止後才能取出，不能把它當一般可自由放入物品的行動箱子。
- 滿載時返回 Home，並先模擬 Home 是否能完整接收所有攜帶種類；不能完整放入時不會只卸下一部分。
- 舊世界的單一 `deadrecall_gathering_storage_stack` 會自動讀入新的多種類 storage，不丟失既有物品。
- 只有成功破壞方塊後才消耗燃料與工具耐久。
- 目標失效或最後提交遭拒時會清除破壞裂紋與進度並短暫退避，避免每 tick 重試。
- 安裝 TotemExcavation 後，可把其錘子放入工具槽。不同 Hammer 掉落種類現在可以共用同一個 16-item 背包，不會因第二種掉落不同而被拒絕。
- 目前正式 runtime 的 Copper Golem Hammer 仍採一次一個已授權目標；完整依 Hammer `area_selection` 自動跑區域的 area-job 仍屬後續 OpenSpec 工作。
- 手持已綁定銅扳手時，採集工作區改由 TotemCore 共用框線顯示；青色框線
  只顯示沒有被不透明方塊遮住的部分，不會穿牆。來源、目的地、目前目標與
  阻塞狀態仍沿用各自的粒子提示。

切換模式前必須先停止；從採集切回分類前還要取出工具並清空採集背包。

## 選配 LLM

GUI 可為每隻銅魁儡設定 OpenAI-compatible Chat Completions：

- API URL、API Key、Model 與連線測試。
- 分類模式可為每個目的地設定獨立 Prompt。
- 接受／拒絕結果會快取，可在 GUI 手動調整。
- 採集模式有獨立 Prompt，手動目標優先於 LLM。

API 未設定、逾時或格式錯誤時會安全失敗，不阻塞 Server tick。不要把
API Key 放進公開截圖、issue 或 log。

## 0.1.17 效能行為

- 銅魁儡由 Fabric entity load/unload events 追蹤，不再每秒遍歷世界中的
  村民、動物與其他所有實體。
- 停止的採集銅魁儡只做一次 `STOPPED` 轉換，之後不再執行採集或 LLM warmup。
- 分類路由使用單次 persisted-state snapshot；阻塞且內容未變時以
  10→20→40→80→160→200 ticks 指數退避重新檢查。
- 既有 `deadrecall_*` NBT、Locksmith 權限、loaded-only 規則、燃料／工具、
  restart recovery 與分類 exactly-once 交易語意保持相容。

## 開發與驗證

CI 會用 TotemCore 0.7.13、TotemExcavation 0.1.5 驗證相容性下限，發布流程
使用目前的 TotemCore 0.7.13、TotemExcavation 0.1.8。另會驗證不安裝
TotemExcavation 的 standalone 啟動、Server GameTests 與 headless Client
GameTests。0.1.17 新增 deterministic scheduler、zero-scan target、event lifecycle、
stopped zero-work、navigation cadence 與 sorting backoff regression coverage。
