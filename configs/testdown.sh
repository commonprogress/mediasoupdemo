#!/bin/sh

echo “some text”

function readfile ()
{
#这里`为esc下面的按键符号
 for file in `ls $1`
 do
#这里的-d表示是一个directory，即目录/子文件夹
 if [ -d $1"/"$file ]; then
  #如果子文件夹则递归
   # echo `basename $file`
   readfile $1"/"$file
 else
  #否则就能够读取该文件的地址
#   echo $1"/"$file
  #读取该文件的文件名，basename是提取文件名的关键字
#   echo `basename $file` #文件的名字 包括后缀
#   echo `basename ${file%.*}` #文件的名字
#   echo `basename ${file##*.}` #文件后缀

  # -d 参数判断 $folder 是否存在
   if [ ! -f $1"/"${file%.*} ]; then
      echo “文件不存在 :${file}”
      if [ ${file##*.} == "sha1" ];then
         echo “想要的文件 :${file}”
         #读取文件内容
	 for line in $(cat $1"/"$file)
         do
            #输出文件内容
#             echo "\n $line"
#            echo "wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}"
            wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}
         done
      else
         echo “不是想要的文件 :${file}”
      fi
   else
      echo “test文件已经存在 :${file}”
      if [ ${file##*.} == "sha1" ];then
         echo “test想要的文件 :${file}”
         # 方法一不能输出
         # cat $1"/"$file | while read line
         # do
            #输出文件内容
            # echo "\n"
            # echo $line
            # echo "wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}"
         # done

# 方法二 可以输出
            for line in $(cat $1"/"$file)
            do
            #输出文件内容
            # echo $line
            echo "test wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}"
         done
      else
         echo “test不是想要的文件 :${file}”
      fi
   fi
 fi
 done
}
#函数定义结束，这里用来运行函数
folder="./webrtc_checkout/src/resources"
readfile $folder
