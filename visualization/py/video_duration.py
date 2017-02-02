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

'''
Takes a directory path and outputs stats of video duration
- Total length of all videos in seconds
- Mean length of video length
- Standard deviation of length

Install pymediainfo - https://github.com/sbraz/pymediainfo  
'''

from pymediainfo import MediaInfo

import os
import sys
import numpy as np

if len(sys.argv) < 2:
    print "Usage -"
    print "\t python video_duration.py <path/to/video/dir>"
    sys.exit()

file_path = sys.argv[1]

print "Finding length of all files in ", file_path

durations = []
for f in os.listdir(file_path):
    if not f[-3:] == "mp4":
        continue
    media_info = MediaInfo.parse(file_path+"/"+f)
    #duration in millionseconds
    
    # Only if MediaInfo was able to open video file
    if len(media_info.tracks) > 0 and media_info.tracks[0].duration:
        duration_in_ms = media_info.tracks[0].duration
    
        durations.append(1.0*duration_in_ms/1000)
    else:
        print "Can't open ", file_path+"/"+f

print ""
print "**********************************"
print "Total Duration - ", sum(durations)
print "Average Duration - ", np.average(durations)
print "Standard deviation of whole set - ", np.std(durations)
print "**********************************"

