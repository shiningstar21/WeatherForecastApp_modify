@echo off
REM 文字コードをUTF-8に設定する
chcp 65001

REM 最新のリモート情報を取得
git fetch origin

REM ローカルmainブランチに切り替え
git checkout main

REM リモートのmainブランチをpull
git pull origin main

echo リモートリポジトリの内容をローカルに反映しました。
pause
