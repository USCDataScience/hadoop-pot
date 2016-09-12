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

if len(sys.argv) != 3:
    print "Usage - "
    print "python similarity_heatmap.py /path/to/formatted/similarity number_of_videos"
    print "number_of_videos can be less or equal to number of videos in similarity matrix"
    sys.exit()


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

print "Data loaded"

#use single color blue 
pl.imshow(data, cmap=pl.cm.Blues, interpolation="nearest")

#show color scale
pl.colorbar()

# tpggle to pl.show() for just viewing image
pl.savefig('../data/similarity_heatmap.png')
print "saved in ../data/similarity_heatmap.png"
