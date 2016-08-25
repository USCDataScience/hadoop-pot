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
import pylab as pl
import sys

pl.ioff()

path_to_sim_mat = sys.argv[1]
num_videos = int(sys.argv[2])
print path_to_sim_mat
print num_videos

# load data from formatted_similarity_calc.csv
# skip header
# skip first column so usecols=range(1 , num_videos),
# paint only upper half filling_values=0)
data = np.genfromtxt(path_to_sim_mat,
                  delimiter=",", skip_header=1, usecols=range(1 , num_videos+1),
                  filling_values=0)

#setting picture size enough for num_videos
if num_videos >= 100:
    pl.figure(figsize = (num_videos/10,num_videos/10), dpi = 5)

#use single color blue
pl.pcolor(data, cmap=pl.cm.Blues, alpha=0.8)
#show color scale
pl.colorbar()

# tpggle to pl.show() for just viewing image
pl.savefig('similarity_heatmap.png')
print "saved in similarity_heatmap.png"
