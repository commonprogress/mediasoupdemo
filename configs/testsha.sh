#!/bin/sh

echo “some text”

function readfile ()
{
 for file in `ls $1`
 do
 if [ -d $1"/"$file ]; then
   readfile $1"/"$file
 else
  # -d 参数判断 $folder 是否存在
  echo -e "========"
  if [ ! -f $1"/"${file%.*} ]; then
      if [ ${file##*.} == "sha1" ];then
         echo -e "想要的文件 :${file}"
         #读取文件内容
         # for line in `cat $1"/"$file`
         for line in $(cat $1"/"$file)
         do 
            #输出文件内容
            # echo -e "$line"
#            echo -e "wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}"
           wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}
         done
      else
         echo -e "不是想要的文件 :${file}"
      fi
   else
      if [ ${file##*.} == "sha1" ];then
         echo -e "test想要的文件 :${file}"
         # echo -e "文件"

         # openssl dgst -sha1  $1"/"${file%.*}
         # sha1sum  $1"/"${file%.*}
         
         # for line in `cat $1"/"$file`
         for line in $(cat $1"/"$file)
            do
            #输出文件内容
            # if [[ $(openssl dgst -sha1  $1"/"${file%.*}) =~ $line ]];then
            if [[ $(sha1sum  $1"/"${file%.*}) =~ $line ]];then
               echo -e "包含内容： $line"
            else
               wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}
	      # echo -e "不包含 wget --no-check-certificate https://storage.googleapis.com/chromium-webrtc-resources/${line} -O $1"/"${file%.*}"
            fi
         done
      else
         echo -e "test不是想要的文件 :${file}"
      fi
   fi
 fi
 done
}
#函数定义结束，这里用来运行函数
folder="./webrtc_checkout/src/resources"
readfile $folder

#/Users/mac8/dongxl/work/android/kotlintest/app/src/main/java/com/example/kotlintest
#/Users/mac8/dongxl/work/wire-audio-video-signaling/webrtc/webrtc_checkout/src/resources
