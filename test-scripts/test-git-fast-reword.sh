#!/usr/bin/env sh

REPO_GIT="https://github.com/JetBrains/intellij-community.git"
REPO_NAME="intellij-community"
COMMIT_HEAD_LIKE_ID="HEAD~20"
COMMIT_MSG="SomeCommitMessage"

echo
echo " ==== git fast reword execution time test ===="

EXEC_DIR=$PWD
TEMP_DIR=$(mktemp -d -t test-git-fast-reword-XXXXXXXXXXXX)

echo "Building git-fast-reword..."
./gradlew distTar || exit 1
TAR_PATH=$(find "$PWD/build/distributions/" -name "*.tar" -type f)
tar -xf "$TAR_PATH" -C "$TEMP_DIR"

cd "$TEMP_DIR" || exit 1
GIT_FAST_REWORD_ROOT=$(find . -name "git-fast-reword-*" -type d)

echo
git clone "$REPO_GIT"
REPO_PATH="$TEMP_DIR/$REPO_NAME"

mv "$GIT_FAST_REWORD_ROOT" "$REPO_PATH"
cd "$REPO_PATH" || exit 1

git config --local user.name "SomeUserName"
git config --local user.email "some@user.email"

echo
echo "Measuring 'git-fast-reword' execution time..."
time "$GIT_FAST_REWORD_ROOT/bin/git-fast-reword" "$COMMIT_HEAD_LIKE_ID" "$COMMIT_MSG"

echo
echo "Commit object:"
git cat-file -p "$COMMIT_HEAD_LIKE_ID"

cd "$EXEC_DIR" || exit 1
rm -rf "$TEMP_DIR"
