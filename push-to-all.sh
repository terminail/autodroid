#!/bin/bash
# Script to push to both GitHub and Gitee repositories

echo "Pushing to GitHub..."
git push github main

if [ $? -eq 0 ]; then
    echo "Successfully pushed to GitHub!"
else
    echo "Failed to push to GitHub"
    exit 1
fi

echo "Attempting to push to Gitee..."
git push origin main

if [ $? -eq 0 ]; then
    echo "Successfully pushed to Gitee!"
else
    echo "Failed to push to Gitee (this is expected due to current issues)"
fi

echo "Push process completed!"