# TotemAutomata

Turn vanilla copper golems into configurable sorting and gathering helpers. Give each golem a home copper chest, fuel, destinations or a work area, then choose what it should move or collect. Manual rules work without an AI account; optional language-model classification can help interpret your sorting and gathering instructions.

## What your golems can do

- **Sort storage:** bind destinations in priority order, merge matching items and components, and maintain separate allow/deny filters for each destination. Scroll each filter list independently. A ruleless empty chest does not automatically accept everything; rejected items attempt to return to the source.
- **Feed working furnaces:** furnace, smoker and blast-furnace inputs follow their respective recipes. Inputs must produce a result, fuel belongs in the fuel slot, and output slots stay outputs.
- **Gather selected blocks:** define a work area and block types, supply an appropriate tool, and let the golem collect and return materials. Its shared storage holds **16 items total**, including mixed types—not 16 stacks. A full load returns home, where unloading requires enough room for the complete load.
- **Manage workers directly:** inspect fuel, tools, carried items and rules in the golem GUI. Copper-ingot interaction repairs four health points, consuming one ingot in survival. Connection lines and work-area outlines help you check bindings while holding the bound wrench.

## Getting started

Craft a copper wrench with three copper ingots and one stick: ingots in the top-middle, middle-middle and middle-right slots; the stick goes bottom-left.

1. Right-click a copper golem with the wrench to select it and open management. Sneaking is unnecessary.
2. Right-click a copper chest to set its source/home. Add fuel through the GUI and keep the golem stopped while configuring its mode.
3. For **sorting**, right-click ordinary containers in your preferred destination order and configure their filters. Left-click a bound destination to remove it; left-click the current source chest to unbind it.
4. For **gathering**, right-click an ordinary block for corner A, then sneak-right-click for corner B. Left-click ordinary non-container blocks to toggle their block types as manual targets. Supply a suitable tool, then start the golem.

Keep the golem and its bindings in one dimension. Stop before changing modes; remove its tool and empty gathering storage before returning to sorting. Carried items can be retrieved while stopped, but this storage is not a freely fillable mobile chest.

## Optional AI and privacy

Configure an OpenAI-compatible endpoint, key and model per golem if you want classification. Sorting destinations have individual prompts; gathering has its own prompt, with manual targets taking priority. Decisions are cached and can be adjusted through the GUI.

Requests go to your configured provider and include prompts, item/block identifiers, names and tags; gathering additionally includes expected drops and tool details, while sorting can include reference information. The configured credential authenticates requests. Provider terms and charges may apply. Keep private information out of prompts and protect the key. Manual operation remains available without a service; missing configuration, timeouts or invalid replies do not grant an unsafe action or block the server tick.

## Requirements and boundaries

For **0.1.24**, install on both client and server: Minecraft **26.2**, Fabric Loader **0.19.3+**, Fabric API, Java **25+**, and **[TotemLibrary](https://modrinth.com/mod/totemlibrary) / TotemCore `>=0.7.18 <0.8.0`**.

Work areas allow up to 64 blocks per axis and scan loaded chunks only. Containers, fluids, unbreakable blocks and the home chest are excluded. TotemLocksmith permissions, TotemRemnant portable-container handling and TotemExcavation hammer support are optional integrations. Hammer gathering processes individual authorized targets; a hammer-selected area job is not part of this release.

## 繁體中文

TotemAutomata 把原版銅魁儡變成可設定的分類與採集助手。每隻魁儡可有自己的來源銅箱、目的地、工作區、燃料、工具與規則。手動操作不需要 AI 帳號，也可選擇接入語言模型協助判斷物品與方塊。

### 特色

- **有順序的倉庫分類：**依綁定順序檢查目的地，合併相同物品與資料元件；每個目的地有獨立允許／拒絕清單，可各自捲動。沒有規則的空箱不會接收所有物品；目的地都拒絕時，會嘗試把物品送回來源。
- **遵守配方的熔爐投料：**熔爐、煙燻爐與高爐分別檢查自己的配方，只有能產出結果的物品才進輸入格；燃料與輸出格保留各自用途。
- **指定區域採集：**設定區域與目標方塊、放入合適工具後採集。共享背包能混放不同種類，但**合計只能攜帶 16 個物品，不是 16 組**。滿載返回 Home，確認銅箱能完整接收後才卸貨。
- **直接管理魁儡：**GUI 可查看燃料、工具、攜帶物與規則。用銅錠右鍵修復 4 點生命，生存模式消耗一個銅錠。手持已綁定扳手可查看容器連線與工作區框線。

### 開始使用

銅扳手配方：工作台上中、中中、中右放銅錠，左下放木棒。

1. 手持扳手右鍵銅魁儡，選取並開啟 GUI，不必蹲下。
2. 右鍵銅箱設定來源／Home，在 GUI 加入燃料；設定模式時先保持停止。
3. **分類：**依優先順序右鍵一般容器，設定各目的地規則。左鍵已綁定目的地可移除；左鍵目前來源銅箱可解除來源。
4. **採集：**右鍵普通方塊設定角落 A，蹲下右鍵設定角落 B；左鍵普通非容器方塊切換該種類的手動目標。放入適合工具後啟動。

魁儡與所有綁定位置須在同一維度。切換模式前先停止；從採集切回分類前，還需取出工具並清空攜帶物。停止時可取出採集物，這個背包不能當成任意放入物品的行動箱子。

### 選配 AI 與資料使用

每隻魁儡可設定 OpenAI-compatible API 網址、金鑰與模型。分類目的地各有獨立 Prompt；採集另有自己的 Prompt，手動目標優先。判斷結果會快取，也可在 GUI 調整。

啟用後，請求會送至你設定的服務，包含 Prompt、物品／方塊 ID、名稱與標籤；採集另外包含預期掉落與工具資訊，分類可能包含參考資料。金鑰用於請求驗證。服務可能收費並適用其資料政策；請勿在 Prompt 放入私人資訊，並妥善保護金鑰。沒有服務仍可使用手動規則；設定缺漏、逾時或格式錯誤不會放行不安全動作，也不會阻塞伺服器 tick。

### 安裝與限制

**0.1.24** 需在客戶端與伺服器安裝 Minecraft **26.2**、Fabric Loader **0.19.3+**、Fabric API、Java **25+**，以及 **[TotemLibrary](https://modrinth.com/mod/totemlibrary)／TotemCore `>=0.7.18 <0.8.0`**。

工作區每軸最多 64 格，只搜尋已載入區塊；不採集容器、流體、不可破壞方塊與自己的 Home。TotemLocksmith 鎖權限、TotemRemnant 可攜式容器及 TotemExcavation 錘子皆為選配整合。錘子採集仍逐一處理授權目標，本版不提供按照錘子選區執行整片區域工作的功能。

[Source / 原始碼](https://github.com/Yunitrish006006/TotemAutomata) · [Issues / 問題回報](https://github.com/Yunitrish006006/TotemAutomata/issues)
