# pooled_time_series
PoT java implementation

# Pre-requisites
If you get any errors running brew install opencv related to numpy, please run:

 1. `pip install numpy`

Now move on to OpenCV
 1. `brew install opencv --with-java`
 
The above should leave you with a:

    /usr/local/Cellar/opencv/<VERSION>/share/OpenCV/java

Directory which contains the associated dylib OpenCV dynamic library along with the OpenCV jar file.

# Getting started
 1. `mvn install assembly:assembly`
 2. `java -Djava.library.path=/usr/local/Cellar/opencv/2.4.9/share/OpenCV/java -jar target/pooled-time-series-1.0-SNAPSHOT-jar-with-dependencies.jar`
 
 The above should produce:
 
```
usage: pooled_time_series
 -d,--dir <directory>        A directory with image files in it
 -h,--help                   Print this message.
 -p,--pathfile <path file>   A file containing full absolute paths to
                             videos. Previous default was
                             memex-index_temp.txt
```
So, to call the code e.g., on a directory of files called `data`, you would run (e.g., with OpenCV 2.4.9):

```
java -Djava.library.path=/usr/local/Cellar/opencv/2.4.9/share/OpenCV/java -jar target/pooled-time-series-1.0-SNAPSHOT-jar-with-dependencies.jar -d data
```

Alternatively you can create (independently of this tool) a file with absolute file paths to video files, 1 per line, and then pass it with the `-p` file to the above program.


# Research Background and Detail
This is a source code used in the following conference paper [1].
It includes the pooled time series (PoT) representation framework as well as basic per-frame descriptor extractions including histogram of optical flows (HOF) and histogram of oriented gradients (HOG).
For more detailed information on the approach, please check the paper.

If you take advantage of this code for any academic purpose, please do cite:

[1] M. S. Ryoo, B. Rothrock, and L. Matthies, "Pooled Motion Features for First-Person Videos", IEEE Conference on Computer Vision and Pattern Recognition (CVPR), June 2015.

http://arxiv.org/pdf/1412.6505v2.pdf

@inproceedings{ryoo2015pot,
 title={Pooled Motion Features for First-Person Videos},
 author={M. S. Ryoo and B. Rothrock and L. Matthies},
 booktitle={IEEE Conference on Computer Vision and Pattern Recognition (CVPR)},
 year={2015},
 month={June},
 address={Boston, MA},
}
