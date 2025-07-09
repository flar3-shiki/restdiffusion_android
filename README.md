# SDWebUI Remote

![app_icon.png](app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp)

これは、AUTOMATIC1111氏の [Stable Diffusion WebUI](https://github.com/AUTOMATIC1111/stable-diffusion-webui) を、Androidデバイスからリモートで操作するための非公式クライアントアプリケーションです。

## ✨ 特徴

*   **画像生成:** Txt2Img と Img2Img の両モードに対応。
*   **多彩なパラメータ設定:**
    *   モデル(Checkpoint)、**VAE**、LoRA（複数適用・強度調整可）、Textual Inversion (Embedding) の動的な読み込みと切り替え
    *   Sampler, Steps, CFG Scale, Seed, 画像サイズなど、WebUIの主要なパラメータを網羅
*   **インペインティング:** マスクを描画して、画像の一部だけを修正・再生成。
*   **キュー機能:**
    *   生成設定をジョブとしてキューに追加し、バックグラウンドで連続実行。
    *   実行中のタスクの中断や、後続タスクの一括キャンセルに対応。
*   **プリセット:** よく使うパラメータの組み合わせを名前を付けて保存・呼び出し。
*   **プロンプトスタイル:** 定型文のようにプロンプトを「スタイル」として保存し、簡単に追加する。
*   **高度な履歴管理:**
    *   過去に生成した画像と全パラメータを自動で記録・確認。
    *   プロンプトによる検索、モデルによるフィルタリング。
    *   不要な履歴の削除と、**お気に入り登録機能**。
*   **PNG Info:** 生成画像に埋め込まれたメタデータを読み取り、パラメータを再現。
*   **外部連携:** ギャラリーアプリ等から画像を共有し、直接Img2Imgのベース画像に設定。
*   **豊富な設定項目:**
    *   APIエンドポイント、クレデンシャル情報の保存
    *   ライト/ダークテーマの切り替え
    *   生成済みコンテンツの履歴サムネイルのNSFWコンテンツのぼかし機能

## 🚀 セットアップ方法

このアプリを使用するには、PC上でStable Diffusion WebUIのAPIサーバーが起動しており、スマート��ォンからネットワーク経由でアクセスできる必要があります。

### 1. Stable Diffusion WebUI側の設定

WebUIを起動する際のバッチファイル（`webui-user.bat`など）を編集し、`COMMANDLINE_ARGS`に以下の引数を追加してください。

```bash
# 例: webui-user.bat

...
set COMMANDLINE_ARGS=--listen --api
...
```

*   `--listen`: LAN内の他のデバイス（スマートフォンなど）からのアクセスを許可します。
*   `--api`: API経由での操作を有効にします。

もしWebUIにパスワード認証をかけたい場合は、さらに`--api-auth`引数を追加します。

```bash
# 認証を設定する場合の例
set COMMANDLINE_ARGS=--listen --api --api-auth="user:pass"
```

`user`と`pass`の部分を、ご自身の好きなユーザー名とパスワードに置き換えてください。

### 2. PCのIPアドレスを確認する

WebUIを起動しているPCのローカルIPアドレスを確認します。

*   **Windows:** コマンドプロンプトを開き、`ipconfig`と入力して表示される「IPv4 アドレス」を確認します（例: `192.168.1.10`）。
*   **Mac:** `ifconfig`コマンドや「システム設定」>「ネットワーク」から確認できます。

### 3. アプリ側の設定

1.  本アプリを起動し、サイドメニューから「設定」画面を開きます。
2.  「API Endpoint」の項目に、先ほど確認したPCのIPアドレスとWebUIのポート番号（デフォルトは`7860`）を組み合わせたアドレスを入力します。
    *   例: `http://192.168.1.10:7860`
3.  WebUI側で認証を設定した場合、「API Authentication」の項目に、設定したユーザー名とパスワードを入力します。
4.  設定画面を閉じ、メイン画面に戻ります。「Connect to Server」ボタンを押して接続できればセットアップは完了です。

## 🛠️ 技術スタック

*   **言語:** Kotlin
*   **UI:** Jetpack Compose, Material 3
*   **アーキテクチャ:** MVVM
*   **非同期処理:** Kotlin Coroutines & Flow
*   **API通信:** Retrofit, OkHttp, Gson
*   **データベース:** Room (履歴、プリセット、スタイル、キューの保存)
*   **設定保存:** Jetpack DataStore
*   **ナビゲーショ���:** Jetpack Navigation Compose
*   **画像読み込み:** Coil
*   **バックグラウンド処理:** Foreground Service
*   **UIライブラリ:** `org.burnoutcrew.reorderable` (ドラッグ＆ドロップ並び替え)

## 📄 ライセンス

```
Copyright 2024 [Your Name]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

*このプロジェクトはApache License 2.0の下でライセンスされています。詳細は`LICENSE`ファイルをご覧ください。*
*使用しているオープンソースライブラリのライセンスについては、アプリ内の「Licenses」画面をご確認ください。*

## ⚠️ 免責事項

*   このアプリケーションは非公式なクライアントです。stable-diffusion-webuiプロジェクトとは一切関係ありません。
*   このアプリケーションを使用したことによるいかなる損害についても、開発者は責任を負いません。自己責任でご利用ください。
*   ほとんどすべてのコードをGeminiが生成しています。

---