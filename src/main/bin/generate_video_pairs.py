#!/usr/bin/env python
# 
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import sys

with open(os.path.join(sys.argv[1], 'original_videos.txt')) as in_file:
    videos = in_file.readlines()

video_counts = len(videos)

with open(os.path.join(sys.argv[2], 'videos.txt'), 'w') as out_file:
    for i in range(video_counts):
        for j in range(i, video_counts):
            f = videos[i].strip()
            one = f.split(' ')
            first_video = one[len(one)-1]
            s = videos[j].strip()
            two = s.split(' ') 
            second_video = two[len(two)-1]
            out_file.write(first_video + ',' + second_video + '\n')
