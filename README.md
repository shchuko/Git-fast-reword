[![Build Status](https://travis-ci.com/shchuko/Git-fast-reword.svg?branch=master)](https://travis-ci.com/shchuko/Git-fast-reword)

# Git-fast-reword

A utility helps you to reword commits faster  
Git history to be edited should meet the rule: successor_commit_time >= ancestor_commit_time   

```
usage: Git-fast-reword {COMMIT-ID MSG}|{COMMITS-LIST-FILE-PATH} [OPTIONS]
 -h,--help            Print this help
 -m,--reword-merges   Allow reword merge commits
```

Support csv-like files:
```
HEAD~10,Message for commit HEAD~10
ef652dys,Message for commit ef652dys
```

Examples:
```
./Git-fast-reword HEAD~2 "New HEAD~2 message" -m
./Git-fast-reword HEAD~2^2 "New HEAD~2^2 message"
./Git-fast-reword commitsRewordList.csv 
./Git-fast-reword commitsRewordList.csv --reword-merges
```
