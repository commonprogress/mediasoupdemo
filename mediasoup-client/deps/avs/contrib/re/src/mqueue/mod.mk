#
# mod.mk
#
# Copyright (C) 2010 Creytiv.com
#

ifeq ($(USE_MQUEUE_BYPASS),1)
SRCS	+= mqueue/mqueue_list.c
else
SRCS	+= mqueue/mqueue.c
endif

ifeq ($(OS),win32)
SRCS	+= mqueue/win32/pipe.c
endif
