# DeadRecall Copper Golem OpenSpec

此封包包含銅傀儡的完整目標規格，以及可直接交給 Codex 實作的 OpenSpec change。玩家操作指南請看 `docs/copper-golem/README.md`。

```text
openspec/
├── specs/copper-golem/spec.md
└── changes/copper-golem-operation-modes/
    ├── proposal.md
    ├── design.md
    ├── tasks.md
    └── specs/copper-golem/spec.md
```

## 已確定的設計

- Shift＋右鍵銅傀儡：綁定目前使用的銅板手並開啟 GUI。
- 普通左鍵銅傀儡不再選擇銅傀儡。
- 現有功能成為 `SORTING` 箱子分類模式。
- 新增 `GATHERING` 資源採集模式。
- 採集模式有一個工具欄及一個採集倉庫欄；倉庫為單一 ItemStack，最多 16 個。
- 採集掃描每 tick 最多檢查 512 個候選方塊，成功採集後從上次掃描位置繼續，不會每次重頭掃。
- 採集站位使用銅傀儡自身碰撞箱，不要求玩家兩格高空間。
- `SORTING → GATHERING` 前，分類貨物和未完成搬運必須清空。
- `GATHERING → SORTING` 前，工具欄、採集倉庫及進行中工作必須清空。
- 模式專屬設定不因切換而刪除。
- 採集資源放回明確指定的 Home 銅箱。
- 板手手動規則優先於 LLM。
- 手持板手時的範圍與路徑顯示由客戶端渲染。
- 受傷銅傀儡可用銅錠右鍵修復。
