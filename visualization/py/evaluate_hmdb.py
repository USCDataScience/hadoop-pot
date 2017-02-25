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

import matplotlib
matplotlib.use('Agg')

import numpy as np
import sys
from sklearn.cluster import DBSCAN
import matplotlib.pyplot as plt
import matplotlib.colors as lib_colors
from collections import Counter
import json

N = 5

if len(sys.argv) < 2:
    print "Usage - "
    print "python evaluate_hmdb.py /path/to/formatted/similarity "
    sys.exit()


path_to_sim_mat = sys.argv[1]
## one line for each video minus header
num_videos = sum(1 for line in open(path_to_sim_mat)) - 1

print path_to_sim_mat
print num_videos

# List of all videos
videos = None
with open(path_to_sim_mat) as f:
    videos = f.readline().strip().split(",")[1:]


# List of all categories, last 7th word is category by naming convention
video_categories = []
for vid in videos:
    video_categories.append(vid.split("_")[-7])


# load data from formatted_similarity_calc.csv
# skip header
# skip first column so usecols=range(1 , num_videos),
# paint only upper half filling_values=0)
data = np.genfromtxt(path_to_sim_mat,
                  delimiter=",", skip_header=1, usecols=range(1 , num_videos+1),
                  filling_values=0)

## add matrix with it's transpose to fill lower half  
data = np.triu(data).T + np.triu(data)  
## Diagonal is also added to itself hence resetting it to 1 
np.fill_diagonal(data, 1)

print "Data loaded"

category_to_acc_high = {}
category_to_acc_low = {}

for i in range(num_videos):
    category = video_categories[i]
    
    #create copies of array so we don't disturb original array 
    sim_score_sort = 0+data[i]
    video_categories_sort = video_categories+[]
    
    #sort with similar index
    sim_score_sort, video_categories_sort = (list(x) for x in zip(*sorted(zip(sim_score_sort, video_categories_sort))))
    
    #keep top N and check how many video_categories_sort ==  category
    low_acc = Counter(video_categories_sort[0:N])
    high_acc = Counter(video_categories_sort[-N:]) 
    
    category_to_acc_high[category] = category_to_acc_high.get(category,Counter([]) ) + high_acc
    category_to_acc_low[category] = category_to_acc_low.get(category,Counter([]) ) + low_acc
    

print category_to_acc_high
print category_to_acc_low

    