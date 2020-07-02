/**
 * @file mqueue.c Thread Safe Message Queue
 *
 * Copyright (C) 2010 Creytiv.com
 */
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#include <re_types.h>
#include <re_fmt.h>
#include <re_mem.h>
#include <re_list.h>
//#include <re_lock.h>
#include <re_mqueue.h>
#include "mqueue.h"


#define MAGIC 0x14553399

static struct list g_mql = LIST_INIT;


/**
 * Defines a Thread-safe Message Queue
 *
 * The Message Queue can be used to communicate between two threads. The
 * receiving thread must run the re_main() loop which will be woken up on
 * incoming messages from other threads. The sender thread can be any thread.
 */
struct mqueue {
	mqueue_h *h;
	void *arg;

	//struct lock *lock;
	struct list evl;

	struct le le;
};

struct msg {
	void *data;
	uint32_t magic;
	int id;

	struct le le;
};


static void destructor(void *arg)
{
	struct mqueue *mq = arg;
	
	list_flush(&mq->evl);

	//mem_deref(mq->lock);

	list_unlink(&mq->le);
}

static void msg_destructor(void *arg)
{
	struct msg *msg = arg;

	list_unlink(&msg->le);
}


/**
 * Allocate a new Message Queue
 *
 * @param mqp Pointer to allocated Message Queue
 * @param h   Message handler
 * @param arg Handler argument
 *
 * @return 0 if success, otherwise errorcode
 */
int mqueue_alloc(struct mqueue **mqp, mqueue_h *h, void *arg)
{
	struct mqueue *mq;
	int err = 0;

	if (!mqp || !h)
		return EINVAL;

	mq = mem_zalloc(sizeof(*mq), destructor);
	if (!mq)
		return ENOMEM;

	mq->h   = h;
	mq->arg = arg;

	//err = lock_alloc(&mq->lock);
	//if (err)
	//	goto out;

	list_init(&mq->evl);

	list_append(&g_mql, &mq->le, mq);
	
	*mqp = mq;

	return err;
}


/**
 * Push a new message onto the Message Queue
 *
 * @param mq   Message Queue
 * @param id   General purpose Identifier
 * @param data Application data
 *
 * @return 0 if success, otherwise errorcode
 */
int mqueue_push(struct mqueue *mq, int id, void *data)
{
	struct msg *msg;

	if (!mq)
		return EINVAL;

	if (mq->h)
		mq->h(id, data, mq->arg);

	msg = mem_zalloc(sizeof(*msg), msg_destructor);
	if (!msg)
		return ENOMEM;

#if 0	
	msg->id    = id;
	msg->data  = data;
	msg->magic = MAGIC;

	//lock_write_get(mq->lock);
	list_append(&mq->evl, &msg->le, msg);
	//lock_rel(mq->lock);
#else
	mem_deref(msg);
#endif
	
	
	return 0;
}


static int poll_mqueue(struct mqueue *mq)
{
	struct msg *msg = NULL;

	if (!mq)
		return EINVAL;
	
	for(;;) {
		//lock_write_get(mq->lock);
		msg = list_ledata(mq->evl.head);
		if (msg)
			list_unlink(&msg->le);
		//lock_rel(mq->lock);
		if (!msg)
			break;

		if (msg->magic != MAGIC) {
			/* maybe log something? */
			continue;
		}

		mq->h(msg->id, msg->data, mq->arg);
		mem_deref(msg);
	}

	return 0;
}


int mqueue_poll(void)
{
	struct le *le;

	LIST_FOREACH(&g_mql, le) {
		struct mqueue *mq = le->data;

		poll_mqueue(mq);
	}

	return 0;
}
