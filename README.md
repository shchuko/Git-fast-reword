[![Build Status](https://travis-ci.com/shchuko/Git-fast-reword.svg?branch=master)](https://travis-ci.com/shchuko/Git-fast-reword)

# Git-fast-reword

A utility helps you to reword commits faster  
Git history to be edited should meet the rule: successor_commit_time >= ancestor_commit_time   

```
usage: Git-fast-reword {COMMIT-ID MSG}|{COMMITS-LIST-FILE-PATH} [OPTIONS]
 -h,--help            Print this help
 -m,--reword-merges   Allow reword merge commits
```