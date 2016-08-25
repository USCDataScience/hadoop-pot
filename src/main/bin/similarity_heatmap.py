import numpy as np
import pylab as pl
import sys

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