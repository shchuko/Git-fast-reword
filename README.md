[![Build Status](https://travis-ci.com/shchuko/git-fast-reword.svg?branch=master)](https://travis-ci.com/shchuko/git-fast-reword)

# git-fast-reword

A utility helps you to reword commits faster  
Git history to be edited should meet the rule: successor_commit_time >= ancestor_commit_time   

```
usage: git-fast-reword {COMMIT-ID MSG}|{COMMITS-LIST-FILE-PATH} [OPTIONS]
 -h,--help            Print this help
 -m,--reword-merges   Allow reword merge commits
```

Use csv-like files for multiple reword:
```
HEAD~10,Message for commit HEAD~10
ef652dys,Message for commit ef652dys
```
Shielding with quotes is unsupported (eg. line will be ignored: ```HEAD, "Message, with, commas"```)

Examples:
```
git-fast-reword HEAD~2 "New HEAD~2 message" -m
git-fast-reword HEAD~2^2 "New HEAD~2^2 message"
git-fast-reword commitsRewordList.csv 
git-fast-reword commitsRewordList.csv --reword-merges
```
