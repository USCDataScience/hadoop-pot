#!/usr/bin/env python

import os
import sys

with open(os.path.join(sys.argv[1], 'videos.txt')) as in_file:
    videos = in_file.readlines()

video_counts = len(videos)

with open(os.path.join(sys.argv[2], 'videos.txt'), 'w') as out_file:
    for i in range(video_counts):
        for j in range(i, video_counts):
            first_video = videos[i].strip()
            second_video = videos[j].strip()
            out_file.write(first_video + ',' + second_video + '\n')
