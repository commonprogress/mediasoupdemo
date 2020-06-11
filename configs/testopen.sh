#!/bin/sh


#cd odp/webserver/conf/vhost/

cat ./webrtc_checkout/src/resources/att-downlink.rx.sha1 | while read line
do 
    each $line
#   wget --no-check-certificate https://storage.googleapis.com/chromium-browser-clang/rc/#$line -O rc
done

echo “start text”

while read line
do
    if [ $line = "" ]
    then
        echo "空"
    else  
        echo "非空"
    fi

done < ./webrtc_checkout/src/resources/att-downlink.rx.sha1

echo “mid text”

exec < ./webrtc_checkout/src/resources/att-downlink.rx.sha1
while read line
do
	echo $line
done

echo “end text”

input="./webrtc_checkout/src/resources/att-downlink.rx.sha1"
while IFS= read -r line
do
echo "$line"
done < "$input"


function readfile ()
{
#这里`为esc下面的按键符号
 for file in `ls $1`
 do
#这里的-d表示是一个directory，即目录/子文件夹
 if [ -d $1"/"$file ]; then
  #如果子文件夹则递归
   echo `basename $file`
   readfile $1"/"$file
 else
  #否则就能够读取该文件的地址
  echo $1"/"$file
  #读取该文件的文件名，basename是提取文件名的关键字
  # echo `basename $file` #文件的名字 包括后缀
  # echo `basename ${file%.*}` #文件的名字
  # echo `basename ${file##*.}` #文件后缀
 fi
 done
}
#函数定义结束，这里用来运行函数
folder="./webrtc_checkout/src/resources"
readfile $folder

