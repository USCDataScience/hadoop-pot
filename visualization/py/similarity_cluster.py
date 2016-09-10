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

if len(sys.argv) != 2:
    print "Usage - "
    print "python similarity_heatmap.py /path/to/formatted/similarity "
    sys.exit()


path_to_sim_mat = sys.argv[1]
## one line for each video minus header
num_videos = sum(1 for line in open(path_to_sim_mat)) - 1

print path_to_sim_mat
print num_videos

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

db = DBSCAN(min_samples=1).fit(data)
with open(path_to_sim_mat) as f:
    videos = f.readline().strip().split(",")[1:]

## each index stores it's cluster label 
clusters = db.labels_

## map of cluster labe to set of videos contained by it
video_clusters = {}
for cluster, video in zip(clusters, videos):
    if cluster not in video_clusters:
        video_clusters[cluster] = []
    
    video_clusters[cluster].append(video)
    

print "clusters calculated"

##############################################################################
# Plot result

core_samples_mask = np.zeros_like(clusters, dtype=bool)
core_samples_mask[db.core_sample_indices_] = True

# Black removed and is used for noise instead.
unique_labels = set(clusters)
colors = plt.cm.Spectral(np.linspace(0, 1, len(unique_labels)))

labelCtr = Counter(clusters)

# The slices for pie chart will be ordered and plotted counter-clockwise.
fracs = []

figure1 = plt.figure()
ax1 = figure1.add_axes([0.07,0.25,0.90,0.70])

# initialize d3-hierarchy json
clusterJson = {"children":[],"name": "clusters"}

# Single loop for forming piechart, , cluster image
for k, col in zip(unique_labels, colors):
    ## setting frac for piechart
    fracs.append(labelCtr[k])
    
    ## d3 json
    clusterJsonChild = {"children":[],"name": "cluster"+str(k),"color":lib_colors.rgb2hex(col)}
    for video in video_clusters[k]:
        clusterJsonChild["children"].append({"name": video})
        
    clusterJson["children"].append(clusterJsonChild)
    
    
    ## cluster image
    if k == -1:
        # Black used for noise.
        col = 'k'

    class_member_mask = (clusters == k)

    xy = data[class_member_mask & core_samples_mask]
    ax1.plot(xy[:, 0], xy[:, 1], 'o', markerfacecolor=col,
             markeredgecolor='k', markersize=14)

    xy = data[class_member_mask & ~core_samples_mask]
    ax1.plot(xy[:, 0], xy[:, 1], 'o', markerfacecolor=col,
             markeredgecolor='k', markersize=6)

plt.title('Estimated number of clusters: %d' % len(unique_labels) )


# make a square figure and axes
ax2 = figure1.add_axes([0.4,0.0,0.20,0.20])

ax2.pie(fracs, startangle=90, colors=colors)

plt.savefig('../data/similarity_cluster.png')


with open('../data/similarity_cluster.json', 'w') as fp:
    json.dump(clusterJson, fp)