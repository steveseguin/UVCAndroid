#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "libuvc/libuvc.h"
static unsigned rng=17;
static unsigned char next_byte(void) {rng=rng*1664525u+1013904223u;return rng>>24;}
static unsigned char clamp(int x){return x<0?0:x>255?255:x;}
static int div_floor(int x){return x<0?-((-x+16383)/16384):x/16384;}
static void expected_rgb(const unsigned char *p, unsigned char *rgb){
 int u=p[1]-128,v=p[3]-128;
 int delta[3]={div_floor(22987*v),div_floor(-5636*u-11698*v),div_floor(29049*u)};
 for(int j=0;j<2;j++)for(int c=0;c<3;c++)rgb[j*3+c]=clamp(p[j*2]+delta[c]);
}
int main(void){
 const int widths[]={8,16,32,640}, heights[]={1,2,3,480};
 int tests=0,failed=0,controls=0;size_t wrong_pixels=0;
 for(int wi=0;wi<4;wi++)for(int hi=0;hi<4;hi++)for(int pattern=0;pattern<5;pattern++){
   int w=widths[wi],h=heights[hi];size_t count=(size_t)w*h,bytes=count*2;
   uvc_frame_t *in=uvc_allocate_frame(bytes),*uy=uvc_allocate_frame(bytes);
   uvc_frame_t *rgb=uvc_allocate_frame(count*3),*bgr=uvc_allocate_frame(count*3),*uybgr=uvc_allocate_frame(count*3);
   assert(in&&uy&&rgb&&bgr&&uybgr);
   in->width=uy->width=w;in->height=uy->height=h;in->step=uy->step=w*2;
   in->frame_format=UVC_FRAME_FORMAT_YUYV;uy->frame_format=UVC_FRAME_FORMAT_UYVY;
   in->sequence=49;in->capture_time.tv_sec=11;in->capture_time.tv_usec=17;
   unsigned char *a=in->data,*u=uy->data;
   for(size_t k=0;k<bytes;k+=4){
     a[k]=next_byte();a[k+2]=next_byte();
     if(pattern==0){a[k+1]=a[k+3]=128;}
     else if(pattern==1){a[k+1]=90;a[k+3]=200;}
     else if(pattern==2){a[k+1]=((k/4)%2)?0:255;a[k+3]=((k/4)%2)?255:0;}
     else if(pattern==3){a[k]=a[k+2]=128;a[k+1]=(k*7)%256;a[k+3]=(k*11+37)%256;}
     else{a[k+1]=next_byte();a[k+3]=next_byte();}
     u[k]=a[k+1];u[k+1]=a[k];u[k+2]=a[k+3];u[k+3]=a[k+2];
   }
   unsigned char *copy=malloc(bytes);assert(copy);memcpy(copy,a,bytes);
   memset(bgr->data,0xCD,count*3);
   assert(uvc_yuyv2rgb(in,rgb)==0);assert(uvc_any2bgr(in,bgr)==0);assert(uvc_uyvy2bgr(uy,uybgr)==0);
   assert(bgr->width==w&&bgr->height==h&&bgr->frame_format==UVC_FRAME_FORMAT_BGR);
   assert(bgr->step==(size_t)w*3&&bgr->sequence==49&&bgr->capture_time.tv_usec==17);
   assert(memcmp(a,copy,bytes)==0);free(copy);
   int mismatch=0;
   for(size_t k=0;k<count;k+=2){
     unsigned char e[6];expected_rgb(a+k*2,e);
     for(int j=0;j<2;j++){
       int pixel_bad=0;
       for(int c=0;c<3;c++){
         assert(((unsigned char*)rgb->data)[(k+j)*3+c]==e[j*3+c]);
         assert(((unsigned char*)uybgr->data)[(k+j)*3+c]==e[j*3+2-c]);
         if(((unsigned char*)bgr->data)[(k+j)*3+c]!=e[j*3+2-c])pixel_bad=1;
       }
       mismatch|=pixel_bad;wrong_pixels+=pixel_bad;
     }
   }
   tests++;if(mismatch)failed++;else controls++;
   if(w==8&&h==1&&pattern==3)printf("8-pixel varying chroma BGR bytes: ");
   if(w==8&&h==1&&pattern==3){for(int k=0;k<24;k++)printf("%u%s",((unsigned char*)bgr->data)[k],k==23?"\n":",");}
   in->frame_format=UVC_FRAME_FORMAT_GRAY8;assert(uvc_yuyv2bgr(in,bgr)==UVC_ERROR_INVALID_PARAM);
   in->frame_format=UVC_FRAME_FORMAT_YUYV;
   unsigned char short_data[23];uvc_frame_t short_frame={.data=short_data,.data_bytes=23};
   assert(uvc_yuyv2bgr(in,&short_frame)==UVC_ERROR_NO_MEM);
   uvc_free_frame(in);uvc_free_frame(uy);uvc_free_frame(rgb);uvc_free_frame(bgr);uvc_free_frame(uybgr);
 }
 printf("{\"cases\":%d,\"bgr_pass\":%d,\"bgr_fail\":%d,\"wrong_pixels\":%zu,\"rgb_oracle_pass\":%d,\"uyvy_bgr_oracle_pass\":%d,\"format_rejection_pass\":%d,\"small_output_rejection_pass\":%d,\"stride\":%d}\n",tests,controls,failed,wrong_pixels,tests,tests,tests,tests,USE_STRIDE);
 return failed?1:0;
}
