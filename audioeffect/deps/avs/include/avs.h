/*
* Wire
* Copyright (C) 2016 Wire Swiss GmbH
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
#ifndef AVS_H__
#define AVS_H__

#ifdef __APPLE__
#define AVS_EXPORT __attribute__((visibility("default")))
#else
#ifdef ANDROID
#define AVS_EXPORT __attribute__((visibility("default")))
#else
#ifdef __EMSCRIPTEN__
#define AVS_EXPORT EMSCRIPTEN_KEEPALIVE
#else
#define AVS_EXPORT
#endif
#endif
#endif

#ifdef __cplusplus
extern "C" {
#endif

#include "avs_dict.h"
#include "avs_log.h"
#include "avs_mediamgr.h"
#include "avs_audio_effect.h"
#include "avs_msystem.h"
#include "avs_string.h"

#ifdef __cplusplus
}
#endif


#endif
