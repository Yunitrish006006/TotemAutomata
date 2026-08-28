## 效能與行為更新

- 採集搜尋改為每隻銅魁儡每 tick 最多 32 格、全伺服器合計最多 256 格，並輪替分配避免後面的銅魁儡飢餓。
- 已選定且仍有效的採集目標會保持鎖定，不再每 tick 重新掃描；每隻銅魁儡每 tick 最多對一個候選方塊進行尋路、掉落、容量、權限或 LLM 等昂貴驗證。
- 移除每秒遍歷所有已載入實體的探索流程，改由 Fabric entity load/unload events 追蹤銅魁儡；村民與動物數量不再增加 Automata 的探索成本。
- 採集目標與回家路徑會沿用現有導航；相同目標正常最多每 10 ticks 重算一次，而且每次只嘗試一個目的地。
- 停止的採集銅魁儡在一次 STOPPED 轉換後，不再執行掃描、驗證、尋路、破壞、卸貨或 LLM warmup。
- 分類阻塞重查改為 10→20→40→80→160→200 ticks 指數退避，並以單次 NBT snapshot 判斷路由；取出、放入、退回與 exactly-once 交易順序維持不變。
- 無效或提交時被拒絕的採集目標會清除裂紋／破壞進度並進入短暫退避，避免每 tick 重試。

## 熔爐分類

- 普通熔爐、煙燻爐與高爐會各自查詢 RecipeManager 中對應的配方；只有能產生非空結果的物品才會放入 slot 0。
- slot 1 只接受燃料、slot 2 維持輸出用途；一般容器的分類行為不變。

## 相容性 / Compatibility

- Minecraft 26.2、Fabric Loader 0.19.3+、Java 25+。
- Requires TotemCore `>=0.7.0 <0.8.0`; TotemExcavation `>=0.1.5` remains optional.
- Existing `deadrecall_*` Copper Golem NBT keys, loaded-only behavior, Locksmith permission checks, fuel/tool rules, restart recovery, and sorting exactly-once semantics remain compatible.
- DeadRecall bundle is not required and is not re-enabled; use the standalone Totem modules.
