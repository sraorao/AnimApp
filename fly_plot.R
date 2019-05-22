## load libraries----
library(ggplot2)
library(cowplot)


## analysing video-----
## open output csv file (insert file name)
path1 = read.csv(file="fly.mp4.csv",header = TRUE, quote = "", colClasses=c(NA, NA, NA, "NULL"))


## calculate Euclidean distance per frame
total_distance <- 0
for (i in 1:nrow(path1)){
  total_distance <- c(total_distance, 
                      (sqrt(((path1$x[i] - path1$x[i+1])^2)
                            + ((path1$y[i]-path1$y[i+1])^2))))
  
}


## calculate instantaneous speed (for each frame +/- 5 frames)
rollvelo<-0
for (i in 6:(length(total_distance)-12)){
  rollvelo[i]<-mean(total_distance[(i-5):(i+5)])
}



length(rollvelo)<-nrow(path1)
rollvelo[is.na(rollvelo)] <- 0
col<-cbind(path1,rollvelo)


## plot path of tracked object with instantaneous speed as colour-----
plot.path1 <- ggplot(data = col, aes(x = x, y = y,colour=rollvelo)) +
  geom_point(alpha = 0.3) + 
  ## set colour gradient limits according to relative speed of object
  scale_colour_gradientn(name="Rolling Velocity", colours = c("purple","red","yellow","green","blue"),
                         values=c(1.0,0.7,0.5,0.35,0.2,0),limits=c(0,30),breaks=c(0,10,20.0,30.0)) +
  geom_path() + 
  coord_fixed(xlim = c(0,848), ylim = c(0, 480)) +
  ggtitle(" ")
plot.path1


## save pdf file of plotted path
width <- 6
ggsave(filename = "output_file.pdf", plot = plot.path1, 
       height = width, width = width)


