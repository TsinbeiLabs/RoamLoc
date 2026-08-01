//
// Created by fuqiuluo on 2024/10/15.
//

#ifndef ROAMLOC_SENSOR_HOOK_H
#define ROAMLOC_SENSOR_HOOK_H

#include "android/sensor.h"

// ssize_t SensorEventQueue::write(const sp<BitTube>& tube,
//        ASensorEvent const* events, size_t numEvents)
typedef int64_t (*OriginalSensorEventQueueWriteType)(void*, void*, int64_t);

// void convertToSensorEvent(const Event &src, sensors_event_t *dst);
typedef void (*OriginalConvertToSensorEventType)(void*, void*);

void doSensorHook();

#endif //ROAMLOC_SENSOR_HOOK_H
