#!/usr/bin/env sh

REPO_GIT="https://github.com/JetBrains/intellij-community.git"
REPO_NAME="intellij-community"
COMMIT_HEAD_LIKE_ID="HEAD~20"

EXEC_DIR=$PWD
TEMP_DIR=$(mktemp -d -t test-git-fast-reword-XXXXXXXXXXXX)

cd "$TEMP_DIR" || exit 1

git clone "$REPO_GIT"
REPO_PATH="$TEMP_DIR/$REPO_NAME"
cd "$REPO_PATH" || exit 1

git config --local user.name "SomeUserName"
git config --local user.email "some@user.email"

echo
echo "Measuring 'git rebase --interactive --rebase-merges' execution time..."
# vi auto-exit
git config --local core.editor "vi -c 'wq'"
time git rebase --interactive --rebase-merges "$COMMIT_HEAD_LIKE_ID"

cd "$EXEC_DIR" || exit 1
rm -rf "$TEMP_DIR"
