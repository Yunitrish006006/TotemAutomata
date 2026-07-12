# DeadRecall

DeadRecall 是 Minecraft Fabric 26.2 模組，整合死亡物品保護、可升級背包、銅魁儡分類與採集、煉藥鍋配方、Discord 橋接與 `/back` 死亡座標返回。

## 專案資訊

| 項目 | 內容 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.154.2+26.2 |
| Java | 25 |
| 目前版本 | 2.2.1 |
| 授權 | BSD-3-Clause |

## 主要功能

- 多等級背包：束口袋升級成基礎、標準、進階與獄髓背包，並提供逐級成長的防護。
- 死亡背包：死亡物品會集中生成為死亡背包，帶光柱、永久保存與虛空保護。
- 銅魁儡工作系統：銅板手管理來源銅箱、分類目的地、燃料、模式與 LLM 設定。
- 銅魁儡採集模式：可設定工作區、工具、採集目標與 Prompt，採集後回來源銅箱存放。
- LLM 輔助分類：支援 OpenAI-compatible API、本地 llama.cpp 端點、快取與手動校正。
- 豬糞與煉藥鍋配方：提供硝石、熱可可、櫻花釀等生存製作流程。
- Discord Bridge：遊戲聊天、死亡訊息與伺服器開關狀態可轉送到 Discord。
- `/back`：記錄死亡維度與座標，支援跨維度返回。

## 文件

| 文件 | 用途 |
| --- | --- |
| [DeadRecall模組完整文檔.md](./DeadRecall模組完整文檔.md) | 完整玩家功能、配方、更新日誌與技術整理 |
| [docs/copper-golem/README.md](./docs/copper-golem/README.md) | 銅魁儡與銅板手操作指南 |
| [docs/discord-bridge-setup.md](./docs/discord-bridge-setup.md) | Discord Bridge 部署與設定 |
| [openspec/specs/copper-golem/spec.md](./openspec/specs/copper-golem/spec.md) | 銅魁儡規格與驗收條件 |

## 建置

```bash
./gradlew build
```

正式 JAR 會輸出到：

```text
build/libs/
```

## 版本重點

### v2.2.1

- 改善銅魁儡採集搜尋：每 tick 掃描預算提高到 512 格。
- 採集成功後保留掃描游標，下一個目標會從上次位置後方繼續找。
- 採集站位改用銅魁儡本身碰撞箱，不再硬性要求玩家兩格高空間。
- 補齊銅錠右鍵修復銅魁儡的使用說明。
- 整理銅魁儡玩家指南、完整文檔與 OpenSpec。

### v2.2.0

- 新增 DeadRecall 創造模式頁籤。
- 死亡背包新增紅色定位光柱。
- 新增豬糞互動、熱可可與櫻花釀。
- 新增背包與豬糞相關進度。
- 銅魁儡採集、分類、LLM 與 GUI 功能大幅擴充。

最後更新：2026-07-12
