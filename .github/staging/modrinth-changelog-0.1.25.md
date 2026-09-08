## TotemAutomata 0.1.25

- Fixes Copper Golem menu item duplication and stale counts during shift-click,
  stack merging, and pickup-all by persisting live slot changes correctly.
- Keeps carried-item slot positions stable while extracting items and preserves
  autonomous fuel/storage updates while the menu is open.
- Verified with 50 server GameTests and 85 unit tests, including reopening
  storage, damaged-tool transfers, and extraction denial while gathering runs.

### 繁體中文

- 修正銅魁儡管理介面 Shift 點擊、堆疊合併及收集同類物品時的物品複製與數量不同步。
- 取出物品期間維持採集背包槽位，並正確同步介面開啟期間的燃料消耗與背包變動。
- 通過 50 項伺服器 GameTest 與 85 項單元測試，涵蓋重新開啟背包、工具耐久保留及運作中禁止取出物品。

Minecraft 26.2 · Fabric · Java 25 · TotemCore >=0.7.18 <0.8.0
