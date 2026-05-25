# MH4G 願シミュレーター Android版

PC版（Javaアプリ）をAndroid APKプロジェクトに移植したものです。  
元のCSVデータ・スキルデータ・ゲームロジックをそのまま使用しています。

---

## プロジェクト構成

```
mh4g_android/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/                        ← ゲームデータCSV（そのまま同梱）
│       │   ├── MH4G_SKILL.csv
│       │   ├── MH4G_DECO.csv
│       │   ├── MH4G_CHARM.csv
│       │   ├── MH4G_EQUIP_HEAD.csv
│       │   ├── MH4G_EQUIP_BODY.csv
│       │   ├── MH4G_EQUIP_ARM.csv
│       │   ├── MH4G_EQUIP_WST.csv
│       │   └── MH4G_EQUIP_LEG.csv
│       ├── java/com/mh4g/simulator/
│       │   ├── MainActivity.kt            ← タブ管理・起動
│       │   ├── data/
│       │   │   ├── Models.kt              ← データモデル（Equipment/Skill/Decoration/Charm）
│       │   │   ├── AppData.kt             ← 全データシングルトン
│       │   │   ├── DataLoader.kt          ← CSV読み込み（ShiftJIS対応）
│       │   │   └── PreferenceManager.kt   ← 永続化（マイセット・お守り・除外状態）
│       │   ├── search/
│       │   │   ├── SearchModels.kt        ← 検索条件・結果モデル
│       │   │   ├── SearchEngine.kt        ← 検索エンジン（PC版ロジック移植）
│       │   │   └── SearchViewModel.kt     ← 非同期検索管理
│       │   └── ui/
│       │       ├── fragments/
│       │       │   ├── SearchFragment.kt         ← メイン検索タブ
│       │       │   ├── EquipExcludeFragment.kt   ← 頭/胴/腕/腰/脚 除外・固定タブ
│       │       │   └── OtherFragments.kt         ← 装飾品除外・お守り・マイセット
│       │       ├── adapters/
│       │       │   └── Adapters.kt               ← 各リストアダプター
│       │       └── dialogs/
│       │           ├── Dialogs.kt                ← お守り追加・マイセット保存
│       │           ├── EquipInfoDialog.kt         ← 装備詳細テーブル
│       │           ├── SkillPointTableDialog.kt   ← スキルポイント詳細テーブル
│       │           └── WeaponDecoDialog.kt        ← 武器スロ・装飾品直接指定
│       └── res/
│           ├── layout/                    ← 各画面レイアウトXML
│           └── values/                    ← 文字列・色・テーマ
```

---

## PC版との対応関係

| PC版クラス        | Android版ファイル                   |
|------------------|-------------------------------------|
| ui.Main / ui.e   | MainActivity.kt                     |
| ui.h.K（メイン） | SearchFragment.kt                   |
| ui.a〜g（除外）  | EquipExcludeFragment.kt             |
| ui.b（お守り）   | CharmFragment（OtherFragments.kt）  |
| ui.f（マイセット）| MySetFragment（OtherFragments.kt） |
| d.a（検索ロジック）| SearchEngine.kt                   |
| c.n（検索条件）  | SearchModels.kt                     |
| c.g（検索結果）  | SearchModels.kt                     |
| CSVデータ        | assets/ フォルダに直接配置          |

---

## 実装済み機能（PC版完全対応）

- ✅ スキル選択（絞り込み検索付き）
- ✅ 発動スキル指定・除外スキル指定
- ✅ 性別・剣士/ガンナー フィルター
- ✅ 武器スロット指定（自動/0〜3）
- ✅ 装飾品直接指定（ダイアログ）
- ✅ 検索結果一覧（装備なし数→防御力→耐性でソート）
- ✅ 検索結果詳細（装備・装飾品・発動スキル・ステータス）
- ✅ 空きスロット活用パターン表示
- ✅ 結果右クリック（長押し）メニュー：
  - マイセットに追加
  - スキルポイント詳細テーブル表示
  - 各部位装備の除外・固定
  - 各部位装備の情報表示（テーブル形式）
- ✅ 頭/胴/腕/腰/脚 除外・固定タブ（絞り込み・タイプフィルター）
- ✅ 装飾品除外タブ（絞り込み）
- ✅ お守り登録タブ（種類・スロット・スキル系統・ポイント指定）
- ✅ お守り検索（結果0件時の候補表示）
- ✅ マイセット保存・閲覧・削除・メモ編集
- ✅ 除外・固定・マイセット・お守りの永続保存（アプリ再起動後も保持）
- ✅ 装備情報ダイアログ（防御・耐性・スキルポイント・素材テーブル）
- ✅ スキルポイントテーブル（部位別・装飾品・合計・発動スキル一覧）

---

## ビルド方法

### 必要なもの

- Android Studio Hedgehog (2023.1.1) 以降
- JDK 17以上
- Android SDK API 34
- Kotlin 1.9.x

### 手順

1. **Android Studioで開く**
   ```
   File → Open → mh4g_android フォルダを選択
   ```

2. **Gradle同期**
   ```
   File → Sync Project with Gradle Files
   ```

3. **ビルド＆インストール**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   または実機/エミュレーターを接続して `Run` ボタンを押す

### リリースAPKを作成する場合

```
Build → Generate Signed Bundle / APK
→ APK を選択
→ キーストアを設定してビルド
```

---

## 注意事項

- 本アプリはMH4G（モンスターハンター4G）のファンメイドツールです
- 元のJarと同じCSVデータを使用しているため、データ内容は同一です
- 検索速度は防具数・条件によって数秒かかる場合があります（バックグラウンドスレッドで実行）
